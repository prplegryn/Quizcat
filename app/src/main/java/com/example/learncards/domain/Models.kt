package com.prplegryn.quizcat.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object CardType {
    const val Learning = "learning"
    const val FlashCard = "flash_card"
    const val Mcq = "MCQ"
    const val Cloze = "cloze"
    const val TypedAnswer = "typed_answer"
}

object SessionStatus {
    const val NotStarted = "not_started"
    const val InProgress = "in_progress"
    const val Completed = "completed"
}

object ProgressStatus {
    const val Unanswered = "未回答"
    const val WaitingConfirm = "待二次确认"
    const val Failed = "未通过"
    const val Completed = "已完成"
}

object FirstAnswerResult {
    const val None = "none"
    const val Correct = "correct"
    const val Wrong = "wrong"
    const val Skip = "skip"
}

object ProgressSource {
    const val New = "new"
    const val OldNew = "old_new"
    const val OldDue = "old_due"
}

object OldKnowledgeStatus {
    const val New = "new"
    const val Old = "old"
}

object SourceResultType {
    const val FirstPass = "first_pass"
    const val PassThenFail = "pass_then_fail"
    const val FailThenPass = "fail_then_pass"
    const val HeavyFailThenPass = "heavy_fail_then_pass"
}

object DefaultTips {
    const val FlashSkip = "已跳过，按答错处理。"
    const val McqSkip = "已跳过，按答错处理。"
    const val ClozeSkip = "已跳过，正确答案需要严格匹配。"
    const val TypedSkip = "已跳过，正确答案需要严格匹配。"
    const val CaseMismatch = "注意区分大小写"
    const val FlashCorrect = "已记录为答对。"
    const val FlashWrong = "已记录为答错，继续复习。"
}

enum class AnswerAction {
    Correct,
    Wrong,
    Skip,
    Submit,
}

@Serializable
data class LearningContent(
    val term: String,
    @SerialName("full_name") val fullName: String,
    val definition: String,
)

@Serializable
data class FlashCardContent(
    val term: String,
    @SerialName("full_name") val fullName: String,
    val definition: String,
    @SerialName("skip_tips") val skipTips: String = DefaultTips.FlashSkip,
)

@Serializable
data class McqContent(
    val question: String,
    @SerialName("right_choice") val rightChoice: String,
    @SerialName("right_tips") val rightTips: String,
    @SerialName("wrong_tips") val wrongTips: String,
    @SerialName("skip_tips") val skipTips: String = DefaultTips.McqSkip,
    @SerialName("choice_1") val choice1: String,
    @SerialName("choice_2") val choice2: String,
    @SerialName("choice_3") val choice3: String,
    @SerialName("choice_4") val choice4: String,
)

@Serializable
data class ClozeContent(
    @SerialName("question_sentence_with_empty") val questionSentenceWithEmpty: String,
    @SerialName("fill_answer") val fillAnswer: String,
    @SerialName("wrong_tips") val wrongTips: String,
    @SerialName("skip_tips") val skipTips: String = DefaultTips.ClozeSkip,
)

@Serializable
data class TypedAnswerContent(
    val question: String,
    val answer: String,
    @SerialName("right_tips") val rightTips: String,
    @SerialName("wrong_tips") val wrongTips: String,
    @SerialName("skip_tips") val skipTips: String = DefaultTips.TypedSkip,
)

@Serializable
data class ImportError(
    val path: String,
    val message: String,
)

data class ImportPreview(
    val items: List<com.prplegryn.quizcat.data.ItemEntity>,
    val cards: List<com.prplegryn.quizcat.data.CardEntity>,
    val errors: List<ImportError>,
) {
    val validItemCount: Int = items.size
    val errorCount: Int = errors.size
    val learningCount: Int = cards.count { it.type == CardType.Learning }
    val scoringCount: Int = cards.count { it.isScoring }
}

data class ImportCommitResult(
    val successCount: Int,
    val errorCount: Int,
)

data class AnswerOutcome(
    val feedback: String,
    val result: String,
)

data class AppSettings(
    val theme: String = "system",
    val firstDayNewItemCount: Int = 20,
    val dailyNewItemCountAfterFirstDay: Int = 5,
    val oldDueDelayDays: Int = 7,
    val failedRequiredStreak: Int = 3,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)
