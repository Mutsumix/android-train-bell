package com.andbell.app.player

import com.andbell.app.player.MelodyPlayer.Note

/**
 * 音階の周波数定義
 */
object Pitch {
    const val C4 = 261.63f
    const val D4 = 293.66f
    const val E4 = 329.63f
    const val F4 = 349.23f
    const val G4 = 392.00f
    const val A4 = 440.00f
    const val B4 = 493.88f
    const val C5 = 523.25f
    const val D5 = 587.33f
    const val E5 = 659.25f
    const val F5 = 698.46f
    const val G5 = 783.99f
    const val REST = 0f
}

/**
 * サンプルメロディ定義
 */
object Melodies {
    val departureBell1: List<Note> = listOf(
        Note(Pitch.G4, 1f), Note(Pitch.E4, 1f), Note(Pitch.C4, 1f), Note(Pitch.E4, 1f),
        Note(Pitch.G4, 1f), Note(Pitch.A4, 0.5f), Note(Pitch.G4, 0.5f), Note(Pitch.E4, 2f),
        Note(Pitch.A4, 1f), Note(Pitch.G4, 1f), Note(Pitch.E4, 1f), Note(Pitch.D4, 1f),
        Note(Pitch.E4, 1f), Note(Pitch.G4, 1f), Note(Pitch.C5, 2f),
    )

    val departureBell2: List<Note> = listOf(
        Note(Pitch.E4, 0.75f), Note(Pitch.F4, 0.25f), Note(Pitch.G4, 1f),
        Note(Pitch.A4, 0.75f), Note(Pitch.G4, 0.25f), Note(Pitch.F4, 0.5f), Note(Pitch.E4, 0.5f),
        Note(Pitch.D4, 0.75f), Note(Pitch.E4, 0.25f), Note(Pitch.F4, 1f),
        Note(Pitch.G4, 1f), Note(Pitch.E4, 1f),
        Note(Pitch.A4, 0.75f), Note(Pitch.B4, 0.25f), Note(Pitch.C5, 1f),
        Note(Pitch.B4, 0.5f), Note(Pitch.A4, 0.5f), Note(Pitch.G4, 1f),
        Note(Pitch.F4, 0.5f), Note(Pitch.G4, 0.5f), Note(Pitch.A4, 0.5f), Note(Pitch.G4, 0.5f),
        Note(Pitch.E4, 1f), Note(Pitch.C4, 2f),
    )

    val departureBell3: List<Note> = listOf(
        Note(Pitch.C5, 0.25f), Note(Pitch.G4, 0.25f), Note(Pitch.E4, 0.25f), Note(Pitch.G4, 0.25f),
        Note(Pitch.C5, 0.25f), Note(Pitch.E5, 0.25f), Note(Pitch.G5, 0.25f), Note(Pitch.E5, 0.25f),
        Note(Pitch.D5, 0.25f), Note(Pitch.B4, 0.25f), Note(Pitch.G4, 0.25f), Note(Pitch.B4, 0.25f),
        Note(Pitch.D5, 0.25f), Note(Pitch.G5, 0.5f), Note(Pitch.REST, 0.25f),
        Note(Pitch.E5, 0.25f), Note(Pitch.C5, 0.25f), Note(Pitch.G4, 0.25f), Note(Pitch.C5, 0.25f),
        Note(Pitch.E5, 0.25f), Note(Pitch.G5, 0.25f), Note(Pitch.E5, 0.5f),
        Note(Pitch.D5, 0.5f), Note(Pitch.C5, 0.5f), Note(Pitch.G5, 3f),
    )

    val doorChime1: List<Note> = listOf(
        Note(Pitch.G4, 0.5f), Note(Pitch.E4, 0.5f), Note(Pitch.C4, 1f),
        Note(Pitch.REST, 0.5f),
        Note(Pitch.G4, 0.5f), Note(Pitch.E4, 0.5f), Note(Pitch.C4, 1.5f),
    )

    val doorChime2: List<Note> = listOf(
        Note(Pitch.G4, 0.6f), Note(Pitch.C4, 1f),
        Note(Pitch.REST, 0.4f),
        Note(Pitch.G4, 0.6f), Note(Pitch.C4, 1.5f),
    )

    val doorChime3: List<Note> = listOf(
        Note(Pitch.E4, 0.4f), Note(Pitch.G4, 0.4f), Note(Pitch.C5, 0.4f),
        Note(Pitch.G4, 0.4f), Note(Pitch.E4, 0.4f), Note(Pitch.C4, 0.6f),
        Note(Pitch.REST, 0.4f),
        Note(Pitch.E4, 0.4f), Note(Pitch.G4, 0.4f), Note(Pitch.C5, 0.4f),
        Note(Pitch.G4, 0.4f), Note(Pitch.E4, 0.4f), Note(Pitch.C4, 1f),
    )
}
