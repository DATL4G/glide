package com.bumptech.glide.load.data

import android.content.ContentResolver
import android.content.UriMatcher
import android.net.Uri
import android.provider.ContactsContract
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/** Fetches an [java.io.InputStream] for a local [android.net.Uri].  */
class StreamLocalUriFetcher(resolver: ContentResolver?, uri: Uri?) : LocalUriFetcher<InputStream?>(resolver!!, uri!!) {
    companion object {
        /** A lookup uri (e.g. content://com.android.contacts/contacts/lookup/3570i61d948d30808e537)  */
        private const val ID_CONTACTS_LOOKUP = 1

        /** A contact thumbnail uri (e.g. content://com.android.contacts/contacts/38/photo)  */
        private const val ID_CONTACTS_THUMBNAIL = 2

        /** A contact uri (e.g. content://com.android.contacts/contacts/38)  */
        private const val ID_CONTACTS_CONTACT = 3

        /**
         * A contact display photo (high resolution) uri (e.g.
         * content://com.android.contacts/5/display_photo)
         */
        private const val ID_CONTACTS_PHOTO = 4

        /**
         * Uri for optimized search of phones by number (e.g.
         * content://com.android.contacts/phone_lookup/232323232
         */
        private const val ID_LOOKUP_BY_PHONE = 5

        /** Match the incoming Uri for special cases which we can handle nicely.  */
        private var URI_MATCHER: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*/#", ID_CONTACTS_LOOKUP)
            URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*", ID_CONTACTS_LOOKUP)
            URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#/photo", ID_CONTACTS_THUMBNAIL)
            URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#", ID_CONTACTS_CONTACT)
            URI_MATCHER.addURI(ContactsContract.AUTHORITY, "contacts/#/display_photo", ID_CONTACTS_PHOTO)
            URI_MATCHER.addURI(ContactsContract.AUTHORITY, "phone_lookup/*", ID_LOOKUP_BY_PHONE)
        }
    }

    @Throws(FileNotFoundException::class)
    override fun loadResource(uri: Uri, contentResolver: ContentResolver): InputStream {
        return loadResourceFromUri(uri, contentResolver)
                ?: throw FileNotFoundException("InputStream is null for $uri")
    }

    @Throws(FileNotFoundException::class)
    private fun loadResourceFromUri(uri: Uri, contentResolver: ContentResolver): InputStream? {
        var mutableUri = uri
        return when (URI_MATCHER.match(mutableUri)) {
            ID_CONTACTS_CONTACT -> openContactPhotoInputStream(contentResolver, mutableUri)
            ID_CONTACTS_LOOKUP, ID_LOOKUP_BY_PHONE -> {
                // If it was a Lookup uri then resolve it first, then continue loading the contact uri.
                mutableUri = ContactsContract.Contacts.lookupContact(contentResolver, mutableUri)
                openContactPhotoInputStream(contentResolver, mutableUri)
            }
            ID_CONTACTS_THUMBNAIL, ID_CONTACTS_PHOTO, UriMatcher.NO_MATCH -> contentResolver.openInputStream(mutableUri)
            else -> contentResolver.openInputStream(mutableUri)
        }
    }

    private fun openContactPhotoInputStream(contentResolver: ContentResolver, contactUri: Uri?): InputStream {
        return ContactsContract.Contacts.openContactPhotoInputStream(
                contentResolver, contactUri, true /*preferHighres*/)
    }

    @Throws(IOException::class)
    override fun close(data: InputStream?) {
        data?.close()
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<InputStream?>
        get() = InputStream::class.java as Class<InputStream?>
}