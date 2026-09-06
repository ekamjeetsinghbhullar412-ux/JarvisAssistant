package com.jarvis.assistant.commands

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.pm.ApplicationInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import net.objecthunter.exp4j.ExpressionBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result of executing a command — text Jarvis should speak back, plus whether
 * it succeeded (used for graceful error handling / retries).
 */
data class ExecutionResult(val spokenResponse: String, val success: Boolean = true)

/**
 * Turns a parsed [Command] into a real Android action using public, permitted APIs only.
 * No command here ever bypasses a permission check or the lock screen.
 */
class CommandExecutor(private val context: Context) {

    fun execute(command: Command): ExecutionResult = try {
        when (command) {
            is Command.OpenApp -> openApp(command.appName)
            Command.OpenSettings -> launch(Intent(Settings.ACTION_SETTINGS), "Opening settings.")
            is Command.SetAlarm -> setAlarm(command.hour, command.minute)
            is Command.SetTimer -> setTimer(command.totalSeconds)
            is Command.CreateReminder -> createReminder(command.text)
            is Command.CallContact -> callContact(command.name)
            is Command.SendMessage -> sendMessage(command.name, command.body)
            is Command.SearchWeb -> searchWeb(command.query)
            Command.PlayMedia -> mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY, "Playing music.")
            Command.PauseMedia -> mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE, "Paused.")
            Command.NextTrack -> mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT, "Skipping track.")
            Command.PreviousTrack -> mediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Going back a track.")
            Command.BatteryStatus -> batteryStatus()
            Command.CurrentDateTime -> currentDateTime()
            is Command.Navigate -> navigate(command.destination)
            is Command.CreateCalendarEvent -> createCalendarEvent(command.title, command.naturalTime)
            is Command.Calculate -> calculate(command.expression)
            Command.WhoAreYou -> ExecutionResult(
                "I'm Jarvis, your on-device assistant. I can control apps, set alarms, send messages " +
                    "with your confirmation, and answer questions — all within what Android allows me to do."
            )
            Command.ClearHistory -> ExecutionResult("Conversation history cleared.")
            is Command.Unrecognized -> ExecutionResult(
                "I didn't catch a device command in that, so I'll treat it as a question.",
                success = false
            )
        }
    } catch (e: ActivityNotFoundException) {
        ExecutionResult("I couldn't find an app to do that.", success = false)
    } catch (e: SecurityException) {
        ExecutionResult("I don't have permission to do that yet. Check Jarvis's permissions in Settings.", success = false)
    } catch (e: Exception) {
        ExecutionResult("Something went wrong trying to do that: ${e.message}", success = false)
    }

    private fun launch(intent: Intent, response: String): ExecutionResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ExecutionResult(response)
    }

    private fun openApp(appName: String): ExecutionResult {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.ApplicationInfo::class.java.let { 0 })
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        } ?: return ExecutionResult("I couldn't find an app called $appName.", success = false)

        val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            ?: return ExecutionResult("$appName can't be opened directly.", success = false)
        return launch(launchIntent, "Opening ${pm.getApplicationLabel(match)}.")
    }

    private fun setAlarm(hour: Int, minute: Int): ExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
        }
        return launch(intent, "Alarm set for %02d:%02d.".format(hour, minute))
    }

    private fun setTimer(seconds: Int): ExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return launch(intent, "Timer started for $seconds seconds.")
    }

    private fun createReminder(text: String): ExecutionResult {
        // No universal "reminders" API on Android; the reliable, permission-safe approach is
        // to create a calendar event a few minutes out, or store it locally (see ReminderDao).
        return createCalendarEvent(text, "in 30 minutes")
    }

    /** Requires CALL_PHONE permission AND explicit prior confirmation from the UI layer. */
    private fun callContact(name: String): ExecutionResult {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ExecutionResult("I need call permission first — please grant it in Settings.", success = false)
        }
        val number = ContactLookup.findNumber(context, name)
            ?: return ExecutionResult("I couldn't find $name in your contacts.", success = false)
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        return launch(intent, "Calling $name.")
    }

    /** Requires SEND_SMS permission AND explicit prior confirmation from the UI layer. */
    private fun sendMessage(name: String, body: String): ExecutionResult {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ExecutionResult("I need SMS permission first — please grant it in Settings.", success = false)
        }
        val number = ContactLookup.findNumber(context, name)
            ?: return ExecutionResult("I couldn't find $name in your contacts.", success = false)
        SmsManager.getDefault().sendTextMessage(number, null, body, null, null)
        return ExecutionResult("Message sent to $name.")
    }

    private fun searchWeb(query: String): ExecutionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra("query", query) }
        return launch(intent, "Searching for $query.")
    }

    private fun mediaKey(keyCode: Int, response: String): ExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
        return ExecutionResult(response)
    }

    private fun batteryStatus(): ExecutionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return ExecutionResult("Your battery is at $pct percent.")
    }

    private fun currentDateTime(): ExecutionResult {
        val fmt = SimpleDateFormat("EEEE, MMMM d, h:mm a", Locale.getDefault())
        return ExecutionResult("It's ${fmt.format(Date())}.")
    }

    private fun navigate(destination: String): ExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination)))
        return launch(intent, "Navigating to $destination.")
    }

    private fun createCalendarEvent(title: String, naturalTime: String): ExecutionResult {
        // Simple version: opens the calendar insert UI pre-filled, so the user does final confirmation
        // rather than Jarvis silently writing to their calendar.
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, "Created by Jarvis (\"$naturalTime\")")
        }
        return launch(intent, "Opening a calendar event for $title. Please confirm the time.")
    }

    private fun calculate(expression: String): ExecutionResult = try {
        val sanitized = expression.replace("times", "*").replace("plus", "+")
            .replace("minus", "-").replace("divided by", "/")
        val result = ExpressionBuilder(sanitized).build().evaluate()
        ExecutionResult("That's $result.")
    } catch (e: Exception) {
        ExecutionResult("I couldn't calculate that.", success = false)
    }
}
