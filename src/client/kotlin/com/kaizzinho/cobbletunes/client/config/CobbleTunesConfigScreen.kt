package com.kaizzinho.cobbletunes.client.config

import com.kaizzinho.cobbletunes.client.CobbleTunesClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CyclingButtonWidget
import net.minecraft.text.Text
import java.util.Locale
import kotlin.math.abs

// vanilla ui keeps mod menu optional
class CobbleTunesConfigScreen(
    private val parent: Screen?
) : Screen(Text.translatable("cobbletunes.config.title")) {

    private enum class Page(val translationKey: String) {
        GENERAL("cobbletunes.config.tab.general"),
        AMBIENCE("cobbletunes.config.tab.ambience"),
        TRANSITIONS("cobbletunes.config.tab.transitions"),
        ADVANCED("cobbletunes.config.tab.advanced")
    }

    private var page = Page.GENERAL
    private var working = CobbleTunesClient.config.copy()
    private var saveError = false

    private var panelLeft = 0
    private var panelTop = 0
    private var panelWidth = 0
    private var panelHeight = 0
    private var contentTop = 0

    override fun init() {
        panelWidth =
            (width - 24)
                .coerceAtMost(430)
                .coerceAtLeast(300)

        panelHeight =
            (height - 16)
                .coerceAtMost(248)
                .coerceAtLeast(218)

        panelLeft = (width - panelWidth) / 2
        panelTop = (height - panelHeight) / 2

        addTabs()

        contentTop = panelTop + 62

        when (page) {
            Page.GENERAL -> addGeneralPage()
            Page.AMBIENCE -> addAmbiencePage()
            Page.TRANSITIONS -> addTransitionsPage()
            Page.ADVANCED -> addAdvancedPage()
        }

        addFooterButtons()
    }

    private fun addTabs() {
        val margin = 10
        val gap = 2
        val available = panelWidth - margin * 2
        val tabWidth = (available - gap * (Page.entries.size - 1)) / Page.entries.size
        val tabY = panelTop + 34

        Page.entries.forEachIndexed { index, candidate ->
            val x = panelLeft + margin + index * (tabWidth + gap)

            val button = ButtonWidget.builder(
                Text.translatable(candidate.translationKey)
            ) {
                if (page != candidate) {
                    page = candidate
                    saveError = false
                    clearAndInit()
                }
            }
                .dimensions(x, tabY, tabWidth, 20)
                .build()

            button.active = page != candidate
            addDrawableChild(button)
        }
    }

    private fun addGeneralPage() {
        var row = 0

        addBooleanOption(
            row++,
            "cobbletunes.config.replace_battle",
            working.replaceBattleMusic
        ) { working.replaceBattleMusic = it }

        addBooleanOption(
            row++,
            "cobbletunes.config.evolution_music",
            working.enableEvolutionMusic
        ) { working.enableEvolutionMusic = it }

        addBooleanOption(
            row++,
            "cobbletunes.config.replace_ambience",
            working.replaceAmbience
        ) { working.replaceAmbience = it }

        addBooleanOption(
            row++,
            "cobbletunes.config.replace_menu",
            working.replaceMenuMusic
        ) { working.replaceMenuMusic = it }

        val volumes = (0..200 step 25).map { it / 100f }

        addEnumOption(
            row,
            "cobbletunes.config.music_volume",
            volumes,
            closestFloat(working.musicVolume, volumes, 1.0f),
            { value -> Text.literal("${(value * 100f).toInt()}%") }
        ) { working.musicVolume = it }
    }

    private fun addAmbiencePage() {
        var row = 0

        addBooleanOption(
            row++,
            "cobbletunes.config.shuffle_ambience",
            working.shuffleAmbienceTracks
        ) { working.shuffleAmbienceTracks = it }

        val joinValues = listOf(0f, 2f, 5f, 10f, 15f, 20f, 30f)
        addSecondsOption(
            row++,
            "cobbletunes.config.world_join_silence",
            joinValues,
            working.worldJoinSilenceSeconds,
            10f
        ) { working.worldJoinSilenceSeconds = it }

        val trackGapValues = listOf(0f, 15f, 30f, 45f, 60f, 90f, 120f, 180f, 240f, 300f)
        addSecondsOption(
            row++,
            "cobbletunes.config.track_silence_min",
            trackGapValues,
            working.trackEndSilenceMinSeconds,
            90f
        ) { working.trackEndSilenceMinSeconds = it }

        addSecondsOption(
            row,
            "cobbletunes.config.track_silence_max",
            trackGapValues,
            working.trackEndSilenceMaxSeconds,
            180f
        ) { working.trackEndSilenceMaxSeconds = it }
    }

    private fun addTransitionsPage() {
        var row = 0

        val crossfadeValues = listOf(0f, 0.15f, 0.25f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 4f, 5f)
        addSecondsOption(
            row++,
            "cobbletunes.config.crossfade",
            crossfadeValues,
            working.crossfadeSeconds,
            2.5f
        ) { working.crossfadeSeconds = it }

        val biomeGapValues = listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 8f, 10f, 12f, 15f)
        addSecondsOption(
            row++,
            "cobbletunes.config.biome_silence_min",
            biomeGapValues,
            working.biomeTransitionSilenceMinSeconds,
            4f
        ) { working.biomeTransitionSilenceMinSeconds = it }

        addSecondsOption(
            row,
            "cobbletunes.config.biome_silence_max",
            biomeGapValues,
            working.biomeTransitionSilenceMaxSeconds,
            8f
        ) { working.biomeTransitionSilenceMaxSeconds = it }
    }

    private fun addAdvancedPage() {
        addBooleanOption(
            0,
            "cobbletunes.config.debug_logging",
            working.debugLogging
        ) { working.debugLogging = it }

        val reset = ButtonWidget.builder(
            Text.translatable("cobbletunes.config.reset_defaults")
        ) {
            working = CobbleTunesClientConfig()
            saveError = false
            clearAndInit()
        }
            .dimensions(
                optionX(),
                rowY(3),
                optionWidth(),
                20
            )
            .build()

        addDrawableChild(reset)
    }

    private fun addFooterButtons() {
        val footerY = panelTop + panelHeight - 27
        val gap = 6
        val buttonWidth = 100
        val total = buttonWidth * 2 + gap
        val startX = width / 2 - total / 2

        addDrawableChild(
            ButtonWidget.builder(
                Text.translatable("cobbletunes.config.save")
            ) {
                saveError = !CobbleTunesClient.applyConfig(working)

                if (!saveError) {
                    client?.setScreen(parent)
                }
            }
                .dimensions(startX, footerY, buttonWidth, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(
                Text.translatable("gui.cancel")
            ) {
                client?.setScreen(parent)
            }
                .dimensions(
                    startX + buttonWidth + gap,
                    footerY,
                    buttonWidth,
                    20
                )
                .build()
        )
    }

    private fun addBooleanOption(
        row: Int,
        translationKey: String,
        initial: Boolean,
        setter: (Boolean) -> Unit
    ) {
        val widget =
            CyclingButtonWidget.onOffBuilder(initial)
                .build(
                    optionX(),
                    rowY(row),
                    optionWidth(),
                    20,
                    Text.translatable(translationKey)
                ) { _, value ->
                    setter(value)
                }

        addDrawableChild(widget)
    }

    private fun addSecondsOption(
        row: Int,
        translationKey: String,
        values: List<Float>,
        current: Float,
        fallback: Float,
        setter: (Float) -> Unit
    ) {
        addEnumOption(
            row,
            translationKey,
            values,
            closestFloat(current, values, fallback),
            ::secondsText,
            setter
        )
    }

    private fun <T> addEnumOption(
        row: Int,
        translationKey: String,
        values: Collection<T>,
        initial: T,
        valueText: (T) -> Text,
        setter: (T) -> Unit
    ) {
        val widget =
            CyclingButtonWidget.builder<T> { value ->
                valueText(value)
            }
                .values(values)
                .initially(initial)
                .build(
                    optionX(),
                    rowY(row),
                    optionWidth(),
                    20,
                    Text.translatable(translationKey)
                ) { _, value ->
                    setter(value)
                }

        addDrawableChild(widget)
    }

    private fun optionX(): Int =
        panelLeft + 24

    private fun optionWidth(): Int =
        panelWidth - 48

    private fun rowY(row: Int): Int =
        contentTop + row * 23

    private fun closestFloat(
        current: Float,
        values: List<Float>,
        fallback: Float
    ): Float =
        values.minByOrNull {
            abs(it - current)
        } ?: fallback

    private fun secondsText(value: Float): Text {
        val formatted = if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.ROOT, "%.2f", value)
                .trimEnd('0')
                .trimEnd('.')
        }
        return Text.literal("${formatted}s")
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        context.fill(
            0,
            0,
            width,
            height,
            0x66090A0E
        )

        context.fill(
            panelLeft,
            panelTop,
            panelLeft + panelWidth,
            panelTop + panelHeight,
            0xE8141418.toInt()
        )
        context.fill(
            panelLeft,
            panelTop,
            panelLeft + panelWidth,
            panelTop + 1,
            0xFFB8B8C8.toInt()
        )
        context.fill(
            panelLeft,
            panelTop + panelHeight - 1,
            panelLeft + panelWidth,
            panelTop + panelHeight,
            0xFF50505C.toInt()
        )

        drawCentered(
            context,
            title,
            panelTop + 10,
            0xFFFFFFFF.toInt(),
            true
        )

        drawCentered(
            context,
            Text.translatable(
                "cobbletunes.config.page.${page.name.lowercase()}"
            ),
            panelTop + 23,
            0xFFB9B9C4.toInt(),
            false
        )

        if (page == Page.ADVANCED) {
            renderAdvancedInfo(context)
        }

        super.render(context, mouseX, mouseY, delta)

        if (saveError) {
            drawCentered(
                context,
                Text.translatable("cobbletunes.config.save_failed"),
                panelTop + panelHeight - 39,
                0xFFFF5555.toInt(),
                true
            )
        }
    }

    override fun renderBackground(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        // no bg here
    }

    override fun applyBlur(delta: Float) {
        // no blur here
    }

    private fun renderAdvancedInfo(context: DrawContext) {
        drawCentered(
            context,
            Text.translatable("cobbletunes.config.client_only"),
            rowY(1) + 6,
            0xFFAAAAAF.toInt(),
            false
        )

        drawCentered(
            context,
            Text.translatable("cobbletunes.config.server_hint"),
            rowY(1) + 18,
            0xFF808088.toInt(),
            false
        )
    }

    private fun drawCentered(
        context: DrawContext,
        text: Text,
        y: Int,
        color: Int,
        shadow: Boolean
    ) {
        val x = width / 2 - textRenderer.getWidth(text) / 2

        context.drawText(
            textRenderer,
            text,
            x,
            y,
            color,
            shadow
        )
    }

    override fun close() {
        client?.setScreen(parent)
    }
}
