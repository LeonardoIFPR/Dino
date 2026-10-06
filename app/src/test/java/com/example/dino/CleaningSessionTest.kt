package com.example.dino

import com.example.dino.data.CleaningSession
import com.example.dino.data.CleaningTool
import org.junit.Assert.*
import org.junit.Test

class CleaningSessionTest {
    @Test fun lightBrushCanRecoverWithoutDamage() {
        val session=CleaningSession()
        repeat(32) {
            for(row in 0..8) for(column in 0..8) {
                session.brush(.1f+column*.1f,.1f+row*.1f,.105f,.35f,CleaningTool.BRUSH)
            }
        }
        assertTrue(session.recovered)
        assertEquals(100f,session.integrity,.001f)
    }

    @Test fun exposedFossilBreaksUnderThePick() {
        val session=CleaningSession()
        repeat(180) {session.brush(.5f,.5f,.105f,1f,CleaningTool.PICK)}
        assertTrue(session.broken)
        assertFalse(session.recovered)
    }

    @Test fun uncoveredSmallPatchDoesNotUnlockCollection() {
        val session=CleaningSession()
        repeat(35) {session.brush(.5f,.5f,.105f,.35f,CleaningTool.BRUSH)}
        assertTrue(session.progress>0f)
        assertFalse(session.recovered)
    }

    @Test fun strongBrushCanDamageAnExposedPiece() {
        val session=CleaningSession()
        repeat(60) {session.brush(.5f,.5f,.105f,.95f,CleaningTool.BRUSH)}
        assertTrue(session.integrity<100f)
        assertFalse(session.recovered)
    }

    @Test fun retryRestoresSedimentAndIntegrity() {
        val session=CleaningSession()
        repeat(180) {session.brush(.5f,.5f,.105f,1f,CleaningTool.PICK)}
        session.restart()
        assertEquals(100f,session.integrity,.001f)
        assertEquals(0f,session.progress,.001f)
        assertFalse(session.recovered)
        assertFalse(session.broken)
    }
}
