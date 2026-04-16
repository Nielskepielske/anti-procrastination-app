package com.example.procrastination_detection.domain.discovery

import com.example.procrastination_detection.domain.model.Category

/**
 * A baseline DiscoveryStrategy that classifies unknown apps/URLs using curated keyword lists.
 *
 * This runs fully local with zero network dependency, making it the perfect default fallback
 * strategy. It is intentionally simple — its role is to handle obvious cases (e.g. "youtube",
 * "reddit", "github") so that only truly ambiguous items reach the user's Inbox.
 *
 * Confidence is graduated: a longer specific match gives higher confidence than a short one.
 */
class KeywordMatcherStrategy : DiscoveryStrategy {

    override val strategyName = "KeywordMatcher"

    private val distractingKeywords = listOf(
        "youtube", "reddit", "twitter", "x.com", "instagram", "tiktok",
        "facebook", "netflix", "twitch", "9gag", "discord", "linkedin",
        "hacker news", "imgur", "tumblr", "pinterest", "snapchat"
    )

    private val productiveKeywords = listOf(
        "github", "gitlab", "stackoverflow", "docs", "documentation",
        "notion", "jira", "confluence", "figma", "jetbrains", "intellij",
        "android studio", "vscode", "neovim", "terminal", "zsh", "bash",
        "overleaf", "arxiv", "scholar", "wikipedia", "coursera", "udemy",
        "leetcode", "codewars", "exercism", "claude", "chatgpt", "perplexity"
    )

    override suspend fun classify(contextStr: String): DiscoveryResult {
        val lower = contextStr.lowercase()

        val distractingMatch = distractingKeywords.firstOrNull { lower.contains(it) }
        val productiveMatch = productiveKeywords.firstOrNull { lower.contains(it) }

        return when {
            distractingMatch != null -> DiscoveryResult(
                suggestedCategory = Category.DISTRACTING,
                confidence = (distractingMatch.length / 12f).coerceIn(0.4f, 0.9f),
                strategyName = strategyName
            )
            productiveMatch != null -> DiscoveryResult(
                suggestedCategory = Category.PRODUCTIVE,
                confidence = (productiveMatch.length / 12f).coerceIn(0.4f, 0.9f),
                strategyName = strategyName
            )
            else -> DiscoveryResult(
                suggestedCategory = Category.AMBIGUOUS,
                confidence = 0f,
                strategyName = strategyName
            )
        }
    }
}
