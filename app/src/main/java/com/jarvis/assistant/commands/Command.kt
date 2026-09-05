package com.jarvis.assistant.commands

/**
 * A recognized local intent, parsed out of raw speech text before ever hitting the AI backend.
 * Anything that doesn't match one of these falls through to the AI conversation path.
 */
sealed class Command {
    data class OpenApp(val appName: String) : Command()
    object OpenSettings : Command()
    data class SetAlarm(val hour: Int, val minute: Int) : Command()
    data class SetTimer(val totalSeconds: Int) : Command()
    data class CreateReminder(val text: String) : Command()
    data class CallContact(val name: String) : Command()
    data class SendMessage(val name: String, val body: String) : Command()
    data class SearchWeb(val query: String) : Command()
    object PlayMedia : Command()
    object PauseMedia : Command()
    object NextTrack : Command()
    object PreviousTrack : Command()
    object BatteryStatus : Command()
    object CurrentDateTime : Command()
    data class Navigate(val destination: String) : Command()
    data class CreateCalendarEvent(val title: String, val naturalTime: String) : Command()
    data class Calculate(val expression: String) : Command()
    object WhoAreYou : Command()
    object ClearHistory : Command()
    data class Unrecognized(val rawText: String) : Command()

    /** Commands that must be confirmed with the user before executing. */
    val requiresConfirmation: Boolean
        get() = this is CallContact || this is SendMessage || this is ClearHistory
}
