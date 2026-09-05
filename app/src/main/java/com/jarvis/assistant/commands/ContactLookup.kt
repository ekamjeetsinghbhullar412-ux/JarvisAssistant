package com.jarvis.assistant.commands

import android.content.Context
import android.provider.ContactsContract

/** Resolves a spoken contact name to a phone number using the device's contacts (READ_CONTACTS). */
object ContactLookup {

    fun findNumber(context: Context, name: String): String? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$name%")

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, selection, args, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return cursor.getString(numberIndex)
            }
        }
        return null
    }
}
