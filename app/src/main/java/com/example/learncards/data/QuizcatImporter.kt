package com.prplegryn.quizcat.data

import com.prplegryn.quizcat.domain.CardType
import com.prplegryn.quizcat.domain.ClozeContent
import com.prplegryn.quizcat.domain.DefaultTips
import com.prplegryn.quizcat.domain.FlashCardContent
import com.prplegryn.quizcat.domain.ImportError
import com.prplegryn.quizcat.domain.ImportPreview
import com.prplegryn.quizcat.domain.LearningContent
import com.prplegryn.quizcat.domain.McqContent
import com.prplegryn.quizcat.domain.TypedAnswerContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class QuizcatImporter {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun preview(rawJson: String): ImportPreview {
        if (rawJson.isBlank()) {
            return ImportPreview(emptyList(), emptyList(), listOf(ImportError("$", "JSON 不能为空")))
        }

        val root = runCatching { json.parseToJsonElement(rawJson) }.getOrElse {
            return ImportPreview(emptyList(), emptyList(), listOf(ImportError("$", it.message ?: "JSON 解析失败")))
        }

        val entries = when (root) {
            is JsonArray -> root.toList()
            is JsonObject -> listOf(root)
            else -> return ImportPreview(emptyList(), emptyList(), listOf(ImportError("$", "根节点必须是 item 对象或数组")))
        }

        val now = System.currentTimeMillis()
        val items = mutableListOf<ItemEntity>()
        val cards = mutableListOf<CardEntity>()
        val errors = mutableListOf<ImportError>()
        val seenItemIds = mutableSetOf<String>()

        entries.forEachIndexed { index, element ->
            val path = "[$index]"
            val obj = element.asObjectOrNull()
            if (obj == null) {
                errors += ImportError(path, "数组元素必须是对象")
                return@forEachIndexed
            }

            val itemObj = obj.obj("item")
            val itemId = itemObj?.str("item_id").orEmpty()
            val term = itemObj?.str("term").orEmpty()
            val itemErrors = mutableListOf<ImportError>()

            if (itemObj == null) itemErrors += ImportError("$path.item", "缺少 item")
            if (itemId.isBlank()) itemErrors += ImportError("$path.item.item_id", "item_id 必填")
            if (term.isBlank()) itemErrors += ImportError("$path.item.term", "term 必填")
            if (itemId.isNotBlank() && !seenItemIds.add(itemId)) {
                itemErrors += ImportError("$path.item.item_id", "item_id 重复：$itemId")
            }

            val learningObj = obj.obj("learning/flashcard")
            if (learningObj == null) {
                itemErrors += ImportError("$path.learning/flashcard", "缺少 learning/flashcard")
            } else {
                validateSameItemId(learningObj, itemId, "$path.learning/flashcard.item_id", itemErrors)
            }

            val mcqObj = obj.obj("MCQ")
            if (mcqObj == null) {
                itemErrors += ImportError("$path.MCQ", "缺少 MCQ")
            } else {
                validateSameItemId(mcqObj, itemId, "$path.MCQ.item_id", itemErrors)
                val rightChoice = mcqObj.str("right_choice")
                if (rightChoice !in setOf("choice_1", "choice_2", "choice_3", "choice_4")) {
                    itemErrors += ImportError("$path.MCQ.right_choice", "right_choice 只能是 choice_1 到 choice_4")
                }
            }

            val clozeObj = obj.obj("cloze")
            if (clozeObj == null) {
                itemErrors += ImportError("$path.cloze", "缺少 cloze")
            } else {
                validateSameItemId(clozeObj, itemId, "$path.cloze.item_id", itemErrors)
                val sentence = clozeObj.str("question_sentence_with_empty").ifBlank {
                    clozeObj.str("question_sentence_with_empty ")
                }
                if (!sentence.contains("____")) {
                    itemErrors += ImportError("$path.cloze.question_sentence_with_empty", "必须包含 ____")
                }
            }

            val typedObj = obj.obj("typed_answer_question")
            if (typedObj == null) {
                itemErrors += ImportError("$path.typed_answer_question", "缺少 typed_answer_question")
            } else {
                validateSameItemId(typedObj, itemId, "$path.typed_answer_question.item_id", itemErrors)
                if (typedObj.str("answer").isBlank()) {
                    itemErrors += ImportError("$path.typed_answer_question.answer", "answer 必填")
                }
            }

            if (itemErrors.isNotEmpty()) {
                errors += itemErrors
                return@forEachIndexed
            }

            val safeLearning = requireNotNull(learningObj)
            val safeMcq = requireNotNull(mcqObj)
            val safeCloze = requireNotNull(clozeObj)
            val safeTyped = requireNotNull(typedObj)

            items += ItemEntity(
                itemId = itemId,
                term = term,
                createdAt = now,
                updatedAt = now,
            )

            val learningContent = LearningContent(
                term = safeLearning.str("term").ifBlank { term },
                fullName = safeLearning.str("full_name"),
                definition = safeLearning.str("definition"),
            )
            val flashContent = FlashCardContent(
                term = safeLearning.str("term").ifBlank { term },
                fullName = safeLearning.str("full_name"),
                definition = safeLearning.str("definition"),
                skipTips = safeLearning.str("skip_tips").ifBlank { DefaultTips.FlashSkip },
            )
            val mcqContent = McqContent(
                question = safeMcq.str("question"),
                rightChoice = safeMcq.str("right_choice"),
                rightTips = safeMcq.str("right_tips"),
                wrongTips = safeMcq.str("wrong_tips"),
                skipTips = safeMcq.str("skip_tips").ifBlank { DefaultTips.McqSkip },
                choice1 = safeMcq.str("choice_1"),
                choice2 = safeMcq.str("choice_2"),
                choice3 = safeMcq.str("choice_3"),
                choice4 = safeMcq.str("choice_4"),
            )
            val clozeContent = ClozeContent(
                questionSentenceWithEmpty = safeCloze.str("question_sentence_with_empty").ifBlank {
                    safeCloze.str("question_sentence_with_empty ")
                },
                fillAnswer = safeCloze.str("fill_answer"),
                wrongTips = safeCloze.str("wrong_tips"),
                skipTips = safeCloze.str("skip_tips").ifBlank { DefaultTips.ClozeSkip },
            )
            val typedContent = TypedAnswerContent(
                question = safeTyped.str("question"),
                answer = safeTyped.str("answer"),
                rightTips = safeTyped.str("right_tips"),
                wrongTips = safeTyped.str("wrong_tips"),
                skipTips = safeTyped.str("skip_tips").ifBlank { DefaultTips.TypedSkip },
            )

            cards += card(itemId, CardType.Learning, false, json.encodeToString(learningContent), now)
            cards += card(itemId, CardType.FlashCard, true, json.encodeToString(flashContent), now)
            cards += card(itemId, CardType.Mcq, true, json.encodeToString(mcqContent), now)
            cards += card(itemId, CardType.Cloze, true, json.encodeToString(clozeContent), now)
            cards += card(itemId, CardType.TypedAnswer, true, json.encodeToString(typedContent), now)
        }

        return ImportPreview(items = items, cards = cards, errors = errors)
    }

    private fun validateSameItemId(
        obj: JsonObject,
        expected: String,
        path: String,
        errors: MutableList<ImportError>,
    ) {
        val actual = obj.str("item_id")
        if (actual != expected) {
            errors += ImportError(path, "必须等于 item.item_id")
        }
    }

    private fun card(
        itemId: String,
        type: String,
        isScoring: Boolean,
        contentJson: String,
        now: Long,
    ): CardEntity = CardEntity(
        cardId = "$itemId:$type",
        itemId = itemId,
        type = type,
        isScoring = isScoring,
        contentJson = contentJson,
        createdAt = now,
        updatedAt = now,
    )

    private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonObject.obj(name: String): JsonObject? = this[name] as? JsonObject

    private fun JsonObject.str(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
}
