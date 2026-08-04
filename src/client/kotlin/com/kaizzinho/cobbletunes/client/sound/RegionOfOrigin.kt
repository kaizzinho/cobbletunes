package com.kaizzinho.cobbletunes.client.sound

enum class RegionOfOrigin(val dexRange: IntRange) {
    KANTO(1..151),
    JOHTO(152..251),
    HOENN(252..386),
    SINNOH(387..493),
    UNOVA(494..649),
    KALOS(650..721),
    ALOLA(722..809),
    GALAR(810..898),
    HISUI(899..905),
    PALDEA(906..1025);

    companion object {
        fun fromDexNumber(dexNumber: Int): RegionOfOrigin? =
            entries.firstOrNull { dexNumber in it.dexRange }
    }
}