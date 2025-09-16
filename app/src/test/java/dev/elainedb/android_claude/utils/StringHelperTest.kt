package dev.elainedb.android_claude.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StringHelperTest {

    private lateinit var stringHelper: StringHelper

    @Before
    fun setUp() {
        stringHelper = StringHelper()
    }

    @Test
    fun isPalindrome_shouldReturnTrueForPalindromes() {
        assertTrue(stringHelper.isPalindrome("racecar"))
        assertTrue(stringHelper.isPalindrome("A man a plan a canal Panama"))
        assertFalse(stringHelper.isPalindrome("race a car")) // This is NOT a palindrome when cleaned
        assertTrue(stringHelper.isPalindrome(""))
        assertTrue(stringHelper.isPalindrome("a"))
    }

    @Test
    fun isPalindrome_shouldReturnFalseForNonPalindromes() {
        assertFalse(stringHelper.isPalindrome("hello"))
        assertFalse(stringHelper.isPalindrome("world"))
        assertFalse(stringHelper.isPalindrome("ab"))
    }

    @Test
    fun countWords_shouldReturnCorrectCount() {
        assertEquals(3, stringHelper.countWords("hello world test"))
        assertEquals(1, stringHelper.countWords("hello"))
        assertEquals(0, stringHelper.countWords(""))
        assertEquals(0, stringHelper.countWords("   "))
        assertEquals(4, stringHelper.countWords("  hello   world  test  again  "))
    }

    @Test
    fun reverseWords_shouldReverseWordOrder() {
        assertEquals("world hello", stringHelper.reverseWords("hello world"))
        assertEquals("test world hello", stringHelper.reverseWords("hello world test"))
        assertEquals("single", stringHelper.reverseWords("single"))
        assertEquals("", stringHelper.reverseWords(""))
    }

    @Test
    fun capitalizeWords_shouldCapitalizeFirstLetterOfEachWord() {
        assertEquals("Hello World", stringHelper.capitalizeWords("hello world"))
        assertEquals("This Is A Test", stringHelper.capitalizeWords("this is a test"))
        assertEquals("Already Capitalized", stringHelper.capitalizeWords("Already Capitalized"))
        assertEquals("", stringHelper.capitalizeWords(""))
    }

    @Test
    fun removeVowels_shouldRemoveAllVowels() {
        assertEquals("Hll Wrld", stringHelper.removeVowels("Hello World"))
        assertEquals("Ths s  tst", stringHelper.removeVowels("This is a test"))
        assertEquals("bcdfg", stringHelper.removeVowels("abcdefg"))
        assertEquals("", stringHelper.removeVowels("aeiou"))
        assertEquals("xyz", stringHelper.removeVowels("xyz"))
    }

    @Test
    fun isValidEmail_shouldReturnTrueForValidEmails() {
        assertTrue(stringHelper.isValidEmail("test@example.com"))
        assertTrue(stringHelper.isValidEmail("user.name@domain.co.uk"))
        assertTrue(stringHelper.isValidEmail("user+tag@example.org"))
        assertTrue(stringHelper.isValidEmail("user_name@example.net"))
    }

    @Test
    fun isValidEmail_shouldReturnFalseForInvalidEmails() {
        assertFalse(stringHelper.isValidEmail("invalid-email"))
        assertFalse(stringHelper.isValidEmail("@example.com"))
        assertFalse(stringHelper.isValidEmail("user@"))
        assertFalse(stringHelper.isValidEmail("user@.com"))
        assertFalse(stringHelper.isValidEmail("user@example"))
        assertFalse(stringHelper.isValidEmail(""))
    }
}