package dev.elainedb.android_claude.utils

class StringHelper {

    fun isPalindrome(text: String): Boolean {
        if (text.isEmpty()) return true
        val cleaned = text.lowercase().replace(Regex("[^a-z0-9]"), "")
        return cleaned == cleaned.reversed()
    }

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }

    fun reverseWords(text: String): String {
        return text.split(" ").reversed().joinToString(" ")
    }

    fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) {
                word.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                word
            }
        }
    }

    fun removeVowels(text: String): String {
        return text.replace(Regex("[aeiouAEIOU]"), "")
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
        return emailRegex.matches(email)
    }
}