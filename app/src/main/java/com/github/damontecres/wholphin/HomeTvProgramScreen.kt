package com.github.damontecres.wholphin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

private val HomeTvZone = ZoneId.of("Europe/Prague")

private val ChannelWidth = 150.dp
private val HourWidth = 240.dp
private val MinuteWidth = HourWidth / 60
private val TimelineHeight = 48.dp
private val ProgrammeRowHeight = 78.dp
private val DayWidth = HourWidth * 24

private enum class WindowMove {
    LEFT,
    RIGHT,
    UP,
    DOWN,
}

private data class WindowTarget(
    val channelNumber: Int,
    val move: WindowMove,
)

@Composable
fun HomeTvProgramScreen(
    onBack: () -> Unit,
    onPlay: (HomeTvPlayback) -> Unit,
    viewModel: HomeTvProgramViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current

    var pendingTarget by remember {
        mutableStateOf<WindowTarget?>(null)
    }

    val windowFocusRequester = remember {
        FocusRequester()
    }

    /*
     * Po načtení nového malého okna:
     *
     * - posuneme časovou osu k anchor času,
     * - počkáme na vykreslení karet,
     * - vrátíme focus na cílovou stanici.
     */
    LaunchedEffect(
        state.date,
        state.anchorTime,
        state.anchorChannelNumber,
        state.channels,
    ) {
        if (
            state.loading ||
            state.date == null ||
            state.anchorTime == null ||
            state.channels.isEmpty()
        ) {
            return@LaunchedEffect
        }

        val anchorMinutes =
            parseTimeMinutes(state.anchorTime)

        if (anchorMinutes != null) {
            val targetDp =
                (
                    anchorMinutes * MinuteWidth.value -
                        350f
                    ).coerceAtLeast(0f)

            val targetPx = with(density) {
                targetDp.dp.roundToPx()
            }

            horizontalScroll.scrollTo(targetPx)
        }

        delay(80)

        if (pendingTarget != null) {
            runCatching {
                windowFocusRequester.requestFocus()
            }

            pendingTarget = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 32.dp,
                vertical = 14.dp,
            ),
    ) {
        Row {
            Button(onClick = onBack) {
                Text("← Domů")
            }

            Spacer(Modifier.width(20.dp))

            Text(
                text = "TV Program",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.width(28.dp))

            Button(
                onClick = {
                    pendingTarget = null
                    viewModel.previousDay()
                },
            ) {
                Text("‹ Předchozí")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    pendingTarget = null
                    viewModel.today()
                },
            ) {
                Text("Dnes")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    pendingTarget = null
                    viewModel.nextDay()
                },
            ) {
                Text("Další ›")
            }

            Spacer(Modifier.width(18.dp))

            state.date?.let { date ->
                Text(
                    text = formatDate(date),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            state.loading && state.channels.isEmpty() -> {
                Text("Načítám televizní program…")
            }

            state.error != null && state.channels.isEmpty() -> {
                Column {
                    Text("Program se nepodařilo načíst")

                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.load(state.date)
                        },
                    ) {
                        Text("Zkusit znovu")
                    }
                }
            }

            else -> {
                val channels = state.channels
                val selectedDate = state.date

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .width(ChannelWidth)
                            .height(TimelineHeight),
                    ) {
                        Text(
                            text = "Stanice",
                            modifier = Modifier.padding(
                                start = 4.dp,
                                top = 15.dp,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll),
                    ) {
                        TimeLine(
                            selectedDate = selectedDate,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(verticalScroll),
                ) {
                    Column(
                        modifier = Modifier.width(ChannelWidth),
                    ) {
                        channels.forEach { channel ->
                            ChannelName(channel)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll),
                    ) {
                        ProgrammeGrid(
                            channels = channels,
                            selectedDate = selectedDate,
                            anchorDate = state.date,
                            anchorTime = state.anchorTime,
                            anchorChannelNumber =
                                state.anchorChannelNumber,
                            pendingTarget = pendingTarget,
                            windowFocusRequester =
                                windowFocusRequester,

                            onMoveLeft = {
                                    channel,
                                    date,
                                    time,
                                ->

                                pendingTarget =
                                    WindowTarget(
                                        channelNumber =
                                            channel.number,
                                        move =
                                            WindowMove.LEFT,
                                    )

                                viewModel.moveWindow(
                                    channelNumber =
                                        channel.number,
                                    date = date,
                                    time = time,
                                )
                            },

                            onMoveRight = {
                                    channel,
                                    date,
                                    time,
                                ->

                                pendingTarget =
                                    WindowTarget(
                                        channelNumber =
                                            channel.number,
                                        move =
                                            WindowMove.RIGHT,
                                    )

                                viewModel.moveWindow(
                                    channelNumber =
                                        channel.number,
                                    date = date,
                                    time = time,
                                )
                            },

                            onMoveUp = { channel ->
                                val previousNumber =
                                    channel.number - 1

                                if (previousNumber > 0) {
                                    pendingTarget =
                                        WindowTarget(
                                            channelNumber =
                                                previousNumber,
                                            move =
                                                WindowMove.UP,
                                        )

                                    viewModel.moveWindow(
                                        channelNumber =
                                            previousNumber,
                                        date =
                                            state.date
                                                ?: return@ProgrammeGrid,
                                        time =
                                            state.anchorTime
                                                ?: return@ProgrammeGrid,
                                    )
                                }
                            },

                            onPlay = onPlay,

                            onMoveDown = { channel ->
                                val nextNumber =
                                    channel.number + 1

                                pendingTarget =
                                    WindowTarget(
                                        channelNumber =
                                            nextNumber,
                                        move =
                                            WindowMove.DOWN,
                                    )

                                viewModel.moveWindow(
                                    channelNumber =
                                        nextNumber,
                                    date =
                                        state.date
                                            ?: return@ProgrammeGrid,
                                    time =
                                        state.anchorTime
                                            ?: return@ProgrammeGrid,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelName(
    channel: EpgChannel,
) {
    Box(
        modifier = Modifier
            .width(ChannelWidth)
            .height(ProgrammeRowHeight),
    ) {
        Text(
            text = channel.name,
            modifier = Modifier.padding(
                start = 4.dp,
                top = 25.dp,
                end = 12.dp,
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimeLine(
    selectedDate: String?,
) {
    Box(
        modifier = Modifier
            .width(DayWidth)
            .height(TimelineHeight),
    ) {
        Row(
            modifier = Modifier
                .width(DayWidth)
                .height(TimelineHeight),
        ) {
            repeat(24) { hour ->
                Box(
                    modifier = Modifier
                        .width(HourWidth)
                        .height(TimelineHeight),
                ) {
                    Text(
                        text = "%02d:00".format(hour),
                        modifier = Modifier.padding(
                            start = 6.dp,
                            top = 13.dp,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (isToday(selectedDate)) {
            val nowX = currentTimeX()

            Text(
                text = "TEĎ",
                modifier = Modifier
                    .offset(
                        x = nowX - 13.dp,
                        y = 0.dp,
                    )
                    .zIndex(2f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Box(
                modifier = Modifier
                    .offset(x = nowX)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary
                    )
                    .zIndex(1f),
            )
        }
    }
}

@Composable
private fun ProgrammeGrid(
    channels: List<EpgChannel>,
    selectedDate: String?,
    anchorDate: String?,
    anchorTime: String?,
    anchorChannelNumber: Int,
    pendingTarget: WindowTarget?,
    windowFocusRequester: FocusRequester,
    onMoveLeft: (EpgChannel, String, String) -> Unit,
    onMoveRight: (EpgChannel, String, String) -> Unit,
    onMoveUp: (EpgChannel) -> Unit,
    onMoveDown: (EpgChannel) -> Unit,
    onPlay: (HomeTvPlayback) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(DayWidth)
            .height(
                ProgrammeRowHeight * channels.size
            ),
    ) {
        Column(
            modifier = Modifier.width(DayWidth),
        ) {
            channels.forEachIndexed {
                    channelIndex,
                    channel,
                ->

                ProgrammeRow(
                    channel = channel,
                    channelIndex = channelIndex,
                    channelCount = channels.size,
                    selectedDate = selectedDate,
                    anchorDate = anchorDate,
                    anchorTime = anchorTime,
                    anchorChannelNumber =
                        anchorChannelNumber,
                    pendingTarget = pendingTarget,
                    windowFocusRequester =
                        windowFocusRequester,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onPlay = onPlay,
                )
            }
        }

        if (isToday(selectedDate)) {
            Box(
                modifier = Modifier
                    .offset(x = currentTimeX())
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary
                    )
                    .zIndex(10f),
            )
        }
    }
}

@Composable
private fun ProgrammeRow(
    channel: EpgChannel,
    channelIndex: Int,
    channelCount: Int,
    selectedDate: String?,
    anchorDate: String?,
    anchorTime: String?,
    anchorChannelNumber: Int,
    pendingTarget: WindowTarget?,
    windowFocusRequester: FocusRequester,
    onMoveLeft: (EpgChannel, String, String) -> Unit,
    onMoveRight: (EpgChannel, String, String) -> Unit,
    onMoveUp: (EpgChannel) -> Unit,
    onMoveDown: (EpgChannel) -> Unit,
    onPlay: (HomeTvPlayback) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(DayWidth)
            .height(ProgrammeRowHeight),
    ) {
        if (selectedDate == null) {
            return@Box
        }

        val anchorIndex =
            findAnchorProgrammeIndex(
                programmes = channel.programmes,
                anchorDate = anchorDate,
                anchorTime = anchorTime,
            )

        channel.programmes.forEachIndexed {
                programmeIndex,
                programme,
            ->

            val geometry =
                programmeGeometry(
                    programme = programme,
                    selectedDate = selectedDate,
                ) ?: return@forEachIndexed

            val isFirstProgramme =
                programmeIndex == 0

            val isLastProgramme =
                programmeIndex ==
                    channel.programmes.lastIndex

            val isTopChannel =
                channelIndex == 0

            val isBottomChannel =
                channelIndex == channelCount - 1

            /*
             * Po načtení nového okna vrátíme focus
             * na anchor pořad cílové stanice.
             */
            val targetChannel =
                pendingTarget?.channelNumber
                    ?: anchorChannelNumber

            val shouldRequestFocus =
                pendingTarget != null &&
                    channel.number == targetChannel &&
                    programmeIndex == anchorIndex

            val cardModifier =
                if (shouldRequestFocus) {
                    Modifier.focusRequester(
                        windowFocusRequester
                    )
                } else {
                    Modifier
                }

            ProgramCard(
                programme = programme,
                x = geometry.first,
                width = geometry.second,
                modifier = cardModifier,

                onClick = {
                    createProgrammePlayback(
                        channel = channel,
                        programme = programme,
                    )?.let(onPlay)
                },

                onLeftEdge = {
                    val previousProgramme =
                        channel.programmes.getOrNull(
                            programmeIndex - 1
                        )

                    val anchor =
                        if (previousProgramme != null) {
                            programmeAnchor(
                                previousProgramme
                            )
                        } else {
                            boundaryAnchor(
                                programme = programme,
                                direction = WindowMove.LEFT,
                            )
                        }

                    if (anchor != null) {
                        onMoveLeft(
                            channel,
                            anchor.first,
                            anchor.second,
                        )
                    }
                },

                onRightEdge = {
                    val nextProgramme =
                        channel.programmes.getOrNull(
                            programmeIndex + 1
                        )

                    val anchor =
                        if (nextProgramme != null) {
                            programmeAnchor(
                                nextProgramme
                            )
                        } else {
                            boundaryAnchor(
                                programme = programme,
                                direction = WindowMove.RIGHT,
                            )
                        }

                    if (anchor != null) {
                        onMoveRight(
                            channel,
                            anchor.first,
                            anchor.second,
                        )
                    }
                },

                onTopEdge =
                    if (isTopChannel) {
                        {
                            onMoveUp(channel)
                        }
                    } else {
                        null
                    },

                onBottomEdge =
                    if (isBottomChannel) {
                        {
                            onMoveDown(channel)
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun ProgramCard(
    programme: EpgProgramme,
    x: Dp,
    width: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLeftEdge: (() -> Unit)? = null,
    onRightEdge: (() -> Unit)? = null,
    onTopEdge: (() -> Unit)? = null,
    onBottomEdge: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (
                    event.type !=
                    KeyEventType.KeyDown
                ) {
                    return@onPreviewKeyEvent false
                }

                when {
                    event.key ==
                        Key.DirectionLeft &&
                        onLeftEdge != null -> {

                        onLeftEdge()
                        true
                    }

                    event.key ==
                        Key.DirectionRight &&
                        onRightEdge != null -> {

                        onRightEdge()
                        true
                    }

                    event.key ==
                        Key.DirectionUp &&
                        onTopEdge != null -> {

                        onTopEdge()
                        true
                    }

                    event.key ==
                        Key.DirectionDown &&
                        onBottomEdge != null -> {

                        onBottomEdge()
                        true
                    }

                    else -> false
                }
            }
            .offset(
                x = x,
                y = 2.dp,
            )
            .width(width)
            .height(72.dp),
        scale = CardDefaults.scale(
            focusedScale = 1.04f,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 6.dp,
            ),
        ) {
            Text(
                text =
                    "${programme.start} – ${programme.end}",
                style =
                    MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = programme.title,
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight =
                    if (
                        programme.state == "current"
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/*
 * Najde pořad nejbližší anchor času.
 *
 * Používáme ISO časy, takže funguje i přes půlnoc.
 */
private fun findAnchorProgrammeIndex(
    programmes: List<EpgProgramme>,
    anchorDate: String?,
    anchorTime: String?,
): Int {
    if (
        programmes.isEmpty() ||
        anchorDate == null ||
        anchorTime == null
    ) {
        return 0
    }

    return try {
        val anchor =
            LocalDate.parse(anchorDate)
                .atTime(
                    LocalTime.parse(anchorTime)
                )
                .atZone(HomeTvZone)
                .toInstant()

        val containing =
            programmes.indexOfFirst { programme ->
                val start =
                    OffsetDateTime
                        .parse(programme.startIso)
                        .toInstant()

                val end =
                    OffsetDateTime
                        .parse(programme.endIso)
                        .toInstant()

                !anchor.isBefore(start) &&
                    anchor.isBefore(end)
            }

        if (containing >= 0) {
            containing
        } else {
            val future =
                programmes.indexOfFirst { programme ->
                    OffsetDateTime
                        .parse(programme.startIso)
                        .toInstant()
                        .isAfter(anchor)
                }

            if (future >= 0) {
                future
            } else {
                programmes.lastIndex
            }
        }
    } catch (_: Exception) {
        0
    }
}

/*
 * Vytvoří nový anchor těsně za okrajem
 * právě načteného programu.
 *
 * Díky ISO času se například:
 *
 * 23:59 -> 00:00 dalšího dne
 *
 * vyřeší automaticky.
 */
private fun createProgrammePlayback(
    channel: EpgChannel,
    programme: EpgProgramme,
): HomeTvPlayback? {
    if (programme.state == "future") {
        return null
    }

    return try {
        val startTs =
            OffsetDateTime.parse(programme.startIso)
                .toEpochSecond()

        val endTs =
            OffsetDateTime.parse(programme.endIso)
                .toEpochSecond()

        val separator =
            if (channel.playUrl.contains("?")) {
                "&"
            } else {
                "?"
            }

        HomeTvPlayback(
            title = programme.title,
            url =
                channel.playUrl +
                    separator +
                    "start_ts=$startTs&end_ts=$endTs",
        )
    } catch (_: Exception) {
        null
    }
}

private fun programmeAnchor(
    programme: EpgProgramme,
): Pair<String, String>? {
    return try {
        val start =
            OffsetDateTime.parse(
                programme.startIso
            )

        val end =
            OffsetDateTime.parse(
                programme.endIso
            )

        val durationSeconds =
            java.time.Duration
                .between(start, end)
                .seconds

        val middle =
            start.plusSeconds(
                (durationSeconds / 2)
                    .coerceAtLeast(1)
            )
                .atZoneSameInstant(HomeTvZone)

        middle.toLocalDate().toString() to
            middle.toLocalTime()
                .withSecond(0)
                .withNano(0)
                .format(
                    DateTimeFormatter.ofPattern(
                        "HH:mm"
                    )
                )
    } catch (_: Exception) {
        null
    }
}

private fun boundaryAnchor(
    programme: EpgProgramme,
    direction: WindowMove,
): Pair<String, String>? {
    return try {
        val value =
            when (direction) {
                WindowMove.LEFT ->
                    OffsetDateTime
                        .parse(programme.startIso)
                        .minusMinutes(1)

                WindowMove.RIGHT ->
                    OffsetDateTime
                        .parse(programme.endIso)
                        .plusMinutes(1)

                else -> return null
            }
                .atZoneSameInstant(HomeTvZone)

        value.toLocalDate().toString() to
            value.toLocalTime()
                .withSecond(0)
                .withNano(0)
                .format(
                    DateTimeFormatter.ofPattern(
                        "HH:mm"
                    )
                )
    } catch (_: Exception) {
        null
    }
}

private fun parseTimeMinutes(
    value: String?,
): Int? {
    if (value == null) {
        return null
    }

    return try {
        val time = LocalTime.parse(value)
        time.hour * 60 + time.minute
    } catch (_: Exception) {
        null
    }
}

private fun currentTimeX(): Dp {
    val now = LocalTime.now(HomeTvZone)
    val minutes =
        now.hour * 60 + now.minute

    return MinuteWidth * minutes.toFloat()
}

private fun isToday(
    selectedDate: String?,
): Boolean {
    if (selectedDate == null) {
        return false
    }

    return try {
        LocalDate.parse(selectedDate) ==
            LocalDate.now(HomeTvZone)
    } catch (_: Exception) {
        false
    }
}

private fun programmeGeometry(
    programme: EpgProgramme,
    selectedDate: String,
): Pair<Dp, Dp>? {
    return try {
        val day =
            LocalDate.parse(selectedDate)

        val dayStart =
            day.atStartOfDay()

        val dayEnd =
            day.plusDays(1)
                .atStartOfDay()

        val programmeStart =
            OffsetDateTime
                .parse(programme.startIso)
                .atZoneSameInstant(HomeTvZone)
                .toLocalDateTime()

        val programmeEnd =
            OffsetDateTime
                .parse(programme.endIso)
                .atZoneSameInstant(HomeTvZone)
                .toLocalDateTime()

        val visibleStart =
            if (programmeStart < dayStart) {
                dayStart
            } else {
                programmeStart
            }

        val visibleEnd =
            if (programmeEnd > dayEnd) {
                dayEnd
            } else {
                programmeEnd
            }

        if (visibleEnd <= visibleStart) {
            return null
        }

        val startMinutes =
            ChronoUnit.MINUTES
                .between(
                    dayStart,
                    visibleStart,
                )
                .toFloat()

        val durationMinutes =
            ChronoUnit.MINUTES
                .between(
                    visibleStart,
                    visibleEnd,
                )
                .toFloat()

        val x =
            MinuteWidth * startMinutes

        val width =
            (
                MinuteWidth * durationMinutes -
                    4.dp
                ).coerceAtLeast(8.dp)

        x to width
    } catch (_: Exception) {
        null
    }
}

private fun formatDate(
    value: String,
): String {
    return try {
        val date =
            LocalDate.parse(value)

        val today =
            LocalDate.now(HomeTvZone)

        val prefix =
            when (date) {
                today ->
                    "Dnes"

                today.minusDays(1) ->
                    "Včera"

                today.plusDays(1) ->
                    "Zítra"

                else ->
                    date.dayOfWeek.getDisplayName(
                        TextStyle.SHORT,
                        Locale("cs"),
                    )
            }

        val formatted =
            date.format(
                DateTimeFormatter.ofPattern(
                    "d. M.",
                    Locale("cs"),
                ),
            )

        "$prefix • $formatted"
    } catch (_: Exception) {
        value
    }
}




