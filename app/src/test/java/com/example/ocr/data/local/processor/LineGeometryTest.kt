package com.example.ocr.data.local.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LineGeometryTest {

    private fun line(text: String, l: Int, t: Int, r: Int, b: Int, corners: List<Pt>? = null) =
        DetectedLine(text, Box(l, t, r, b), corners)

    private fun quad(l: Int, t: Int, r: Int, b: Int, tilt: Int = 0) =
        listOf(Pt(l, t), Pt(r, t + tilt), Pt(r, b + tilt), Pt(l, b))

    // isValidTextLine

    @Test
    fun `valid line passes filter`() {
        assertTrue(LineGeometry.isValidTextLine(line("Hello world", 0, 0, 200, 30), 1000, 1000))
    }

    @Test
    fun `rejects too short, symbol-only, tiny or oversized lines`() {
        assertFalse(LineGeometry.isValidTextLine(line("a", 0, 0, 200, 30), 1000, 1000))
        assertFalse(LineGeometry.isValidTextLine(line("--==", 0, 0, 200, 30), 1000, 1000))
        assertFalse(LineGeometry.isValidTextLine(line("Hello", 0, 0, 200, 5), 1000, 1000))
        assertFalse(LineGeometry.isValidTextLine(line("Hello", 0, 0, 200, 400), 1000, 1000))
        assertFalse(LineGeometry.isValidTextLine(line("Hello", 0, 0, 20, 30), 1000, 1000))
    }

    @Test
    fun `rejects lines that are mostly punctuation`() {
        assertFalse(LineGeometry.isValidTextLine(line("a.........", 0, 0, 200, 30), 1000, 1000))
    }

    // filterStrictlyContained

    @Test
    fun `removes box fully inside a bigger one and keeps disjoint boxes`() {
        val outer = line("outer", 0, 0, 300, 50)
        val inner = line("inner", 10, 10, 100, 40)
        val other = line("other", 0, 100, 300, 150)

        val result = LineGeometry.filterStrictlyContained(listOf(inner, outer, other))

        assertEquals(setOf(outer, other), result.toSet())
    }

    @Test
    fun `partially overlapping boxes are both kept`() {
        val a = line("aa", 0, 0, 200, 50)
        val b = line("bb", 150, 10, 350, 60)
        assertEquals(2, LineGeometry.filterStrictlyContained(listOf(a, b)).size)
    }

    // canMerge / mergeLines

    @Test
    fun `merges straight fragments on the same row with a small gap`() {
        val left = line("Hello", 0, 100, 200, 130, quad(0, 100, 200, 130))
        val right = line("world", 230, 102, 400, 132, quad(230, 102, 400, 132))
        assertTrue(LineGeometry.canMerge(left, right))
    }

    @Test
    fun `does not merge when horizontal gap is too large`() {
        val left = line("Hello", 0, 100, 200, 130)
        val right = line("world", 200 + LineGeometry.MERGE_GAP_X + 1, 100, 500, 130)
        assertFalse(LineGeometry.canMerge(left, right))
    }

    @Test
    fun `does not merge fragments on different rows`() {
        val top = line("Hello", 0, 100, 200, 130)
        val below = line("world", 210, 140, 400, 170)
        assertFalse(LineGeometry.canMerge(top, below))
    }

    @Test
    fun `merges tilted fragments whose edges line up`() {
        // Left fragment slopes down 20px; right one continues from where it ends
        val left = line("Hello", 0, 100, 200, 150, quad(0, 100, 200, 130, tilt = 20))
        val right = line("world", 210, 120, 400, 170, quad(210, 120, 400, 150, tilt = 20))
        assertTrue(LineGeometry.canMerge(left, right))
    }

    @Test
    fun `does not merge tilted fragments with a vertical jump`() {
        val left = line("Hello", 0, 100, 200, 150, quad(0, 100, 200, 130, tilt = 20))
        val right = line("world", 210, 160, 400, 210, quad(210, 160, 400, 190, tilt = 20))
        assertFalse(LineGeometry.canMerge(left, right))
    }

    @Test
    fun `mergeLines groups a row of fragments and leaves other rows alone`() {
        val a = line("one", 0, 0, 100, 30)
        val b = line("two", 120, 0, 220, 30)
        val c = line("three", 240, 0, 340, 30)
        val d = line("next", 0, 100, 100, 130)

        val groups = LineGeometry.mergeLines(listOf(d, c, a, b))

        assertEquals(2, groups.size)
        assertEquals(listOf(a, b, c), groups.first { it.size == 3 })
        assertEquals(listOf(d), groups.first { it.size == 1 })
    }

    // sortReadingOrder

    @Test
    fun `sorts groups top to bottom then left to right`() {
        val bottom = listOf(line("c", 0, 200, 100, 230))
        val topRight = listOf(line("b", 500, 3, 600, 33))
        val topLeft = listOf(line("a", 0, 0, 100, 30))

        val sorted = LineGeometry.sortReadingOrder(listOf(bottom, topRight, topLeft))

        assertEquals(listOf(topLeft, topRight, bottom), sorted)
    }

    // mergedQuad

    @Test
    fun `mergedQuad spans the outer corners of the group`() {
        val a = line("aa", 0, 0, 100, 30, quad(0, 0, 100, 30))
        val b = line("bb", 120, 0, 220, 30, quad(120, 0, 220, 30))

        assertEquals(
            listOf(Pt(0, 0), Pt(220, 0), Pt(220, 30), Pt(0, 30)),
            LineGeometry.mergedQuad(listOf(a, b)),
        )
    }

    @Test
    fun `mergedQuad is null when a line has no corners`() {
        val a = line("aa", 0, 0, 100, 30, quad(0, 0, 100, 30))
        val b = line("bb", 120, 0, 220, 30)
        assertNull(LineGeometry.mergedQuad(listOf(a, b)))
    }
}
