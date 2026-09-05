package com.jarvis.assistant.commands

import java.util.Locale

/**
 * Lightweight, fully offline intent parser. Runs before any network call so that
 * common device actions never depend on internet access or the AI backend.
 *
 * This is intentionally simple (keyword + regex based) rather than a full NLU model —
 * it is fast, free, transparent, and easy for you to extend with new phrases.
 */
object CommandParser {

    private val alarmRegex = Regex("""set (?:an? )?alarm (?:for )?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
    private val timerRegex = Regex("""(?:start a |set a )?timer for (\d+)\s*(second|minute|hour)s?""")
    private val openAppRegex = Regex("""open (.+)""")
    private val callRegex = Regex("""call (.+)""")
    private val messageRegex = Regex("""(?:send a message to|text) (\w+)(?: saying| that says)? (.+)""")
    private val reminderRegex = Regex("""remind me to (.+)""")
    private val searchRegex = Regex("""search (?:the web )?for (.+)""")
    private val navigateRegex = Regex("""(?:navigate to|directions to|take me to) (.+)""")
    private val calendarRegex = Regex("""(?:create|add) (?:an? )?(?:event|meeting) (.+?) (?:at|on|for) (.+)""")
    private val calculateRegex = Regex("""(?:calculate|what is|what's) (.+)""")

    fun parse(rawText: String): Command {
        val text = rawText.trim().lowercase(Locale.getDefault())

        alarmRegex.find(text)?.let { m ->
            var hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val meridiem = m.groupValues[3]
            if (meridiem == "pm" && hour < 12) hour += 12
            if (meridiem == "am" && hour == 12) hour = 0
            return Command.SetAlarm(hour, minute)
        }

        timerRegex.find(text)?.let { m ->
            val amount = m.groupValues[1].toInt()
            val unit = m.groupValues[2]
            val seconds = when (unit) {
                "hour" -> amount * 3600
                "minute" -> amount * 60
                else -> amount
            }
            return Command.SetTimer(seconds)
        }

        if (text.contains("open settings")) return Command.OpenSettings
        if (text.contains("who are you") || text.contains("what are you")) return Command.WhoAreYou
        if (text.contains("battery")) return Command.BatteryStatus
        if (text.contains("what time") || text.contains("what's the date") || text.contains("what day")) {
            return Command.CurrentDateTime
        }
        if (text.contains("clear") && text.contains("history")) return Command.ClearHistory
        if (text.contains("play") && (text.contains("music") || text.contains("song"))) return Command.PlayMedia
        if (text.contains("pause")) return Command.PauseMedia
        if (text.contains("next song") || text.contains("skip")) return Command.NextTrack
        if (text.contains("previous song") || text.contains("last track")) return Command.PreviousTrack

        messageRegex.find(text)?.let { m -> return Command.SendMessage(m.groupValues[1], m.groupValues[2]) }
        callRegex.find(text)?.let { m -> return Command.CallContact(m.groupValues[1].trim()) }
        reminderRegex.find(text)?.let { m -> return Command.CreateReminder(m.groupValues[1].trim()) }
        searchRegex.find(text)?.let { m -> return Command.SearchWeb(m.groupValues[1].trim()) }
        navigateRegex.find(text)?.let { m -> return Command.Navigate(m.groupValues[1].trim()) }
        calendarRegex.find(text)?.let { m -> return Command.CreateCalendarEvent(m.groupValues[1].trim(), m.groupValues[2].trim()) }
        calculateRegex.find(text)?.let { m -> return Command.Calculate(m.groupValues[1].trim()) }
        openAppRegex.find(text)?.let { m -> return Command.OpenApp(m.groupValues[1].trim()) }

        return Command.Unrecognized(rawText)
    }
}
