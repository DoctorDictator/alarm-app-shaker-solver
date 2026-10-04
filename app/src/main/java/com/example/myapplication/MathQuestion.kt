package com.example.myapplication

data class MathQuestion(val firstFactor: Int, val secondFactor: Int) {
    val prompt: String get() = "$firstFactor × $secondFactor = ?"
    fun accepts(input: String): Boolean = input.trim().toIntOrNull() == firstFactor * secondFactor
}
