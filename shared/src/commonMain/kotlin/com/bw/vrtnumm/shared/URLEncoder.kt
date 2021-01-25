package com.bw.vrtnumm.shared

/*
  * Written using on-line Java Platform 1.2/1.4 API Specification, as well
  * as "The Java Class Libraries", 2nd edition (Addison-Wesley, 1998).
  * Status:  Believed complete and correct.
  */
/**
 * This utility class contains static methods that converts a
 * string into a fully encoded URL string in x-www-form-urlencoded
 * format.  This format replaces certain disallowed characters with
 * encoded equivalents.  All upper case and lower case letters in the
 * US alphabet remain as is, the space character (' ') is replaced with
 * '+' sign, and all other characters are converted to a "%XX" format
 * where XX is the hexadecimal representation of that character in a
 * certain encoding (by default, the platform encoding, though the
 * standard is "UTF-8").
 *
 *
 * This method is very useful for encoding strings to be sent to CGI scripts
 *
 * @author Aaron M. Renn (arenn@urbanophile.com)
 * @author Warren Levy (warrenl@cygnus.com)
 * @author Mark Wielaard (mark@klomp.org)
 */
object URLEncoder {
    /**
     * This method translates the passed in string into x-www-form-urlencoded
     * format using the character encoding to hex-encode the unsafe characters.
     *
     * @param s The String to convert
     * @param encoding The encoding to use for unsafe characters
     *
     * @return The converted String
     *
     * @exception UnsupportedEncodingException If the named encoding is not
     * supported
     *
     * @since 1.4
     */
    fun encode(s: String): String {
        val length = s.length
        var start = 0
        var i = 0
        val result = StringBuilder(length)
        while (true) {
            while (i < length && isSafe(s[i])) i++

            // Safe character can just be added
            result.append(s.substring(start, i))

            // Are we done?
            if (i >= length) return result.toString() else if (s[i] == ' ') {
                result.append('+') // Replace space char with plus symbol.
                i++
            } else {
                // Get all unsafe characters
                start = i
                var c: Char = ' '
                while (i < length && s[i].also { c = it } != ' ' && !isSafe(c)) i++

                // Convert them to %XY encoded strings
                val unsafe = s.substring(start, i)
                val bytes = unsafe.encodeToByteArray()
                for (j in bytes.indices) {
                    result.append('%')
                    val `val` = bytes[j].toInt()
                    result.append(hex[`val` and 0xf0 shr 4])
                    result.append(hex[`val` and 0x0f])
                }
            }
            start = i
        }
    }

    /**
     * Private static method that returns true if the given char is either
     * a uppercase or lowercase letter from 'a' till 'z', or a digit froim
     * '0' till '9', or one of the characters '-', '_', '.' or '*'. Such
     * 'safe' character don't have to be url encoded.
     */
    private fun isSafe(c: Char): Boolean {
        return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_' || c == '.' || c == '*')
    }

    /**
     * Used to convert to hex.  We don't use Integer.toHexString, since
     * it converts to lower case (and the Sun docs pretty clearly
     * specify upper case here), and because it doesn't provide a
     * leading 0.
     */
    private const val hex = "0123456789ABCDEF"
}