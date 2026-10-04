package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class MathQuestionTest {
    @Test fun displayedSevenTimesFiveAcceptsThirtyFive() {
        val question = MathQuestion(7, 5)
        assertEquals("7 × 5 = ?", question.prompt)
        assertTrue(question.accepts("35"))
        assertFalse(question.accepts("34"))
    }
    @Test fun allGeneratedMultiplicationsAcceptTheirProduct() {
        for (first in 2..9) for (second in 2..9) {
            assertTrue(MathQuestion(first, second).accepts((first * second).toString()))
        }
    }
    @Test fun emptyAndMalformedAnswersAreRejected() {
        val question = MathQuestion(7, 5)
        listOf("", " ", "35.0", "abc", "999999999999999").forEach { assertFalse(question.accepts(it)) }
    }
    @Test fun whitespaceAroundAnswerIsAccepted() {
        assertTrue(MathQuestion(7, 5).accepts(" 35\n"))
    }
}
