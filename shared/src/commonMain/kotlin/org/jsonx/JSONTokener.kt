/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jsonx

// Note: this class was written without inspecting the non-free org.json sourcecode.
/**
 * Parses a JSON ([RFC 4627](http://www.ietf.org/rfc/rfc4627.txt))
 * encoded string into the corresponding object. Most clients of
 * this class will use only need the [constructor][.JSONTokener]
 * and [.nextValue] method. Example usage: <pre>
 * String json = "{"
 * + "  \"query\": \"Pizza\", "
 * + "  \"locations\": [ 94043, 90210 ] "
 * + "}";
 *
 * JSONObject object = (JSONObject) new JSONTokener(json).nextValue();
 * String query = object.getString("query");
 * JSONArray locations = object.getJSONArray("locations");</pre>
 *
 *
 * This parser is lenient. A successful parse does not necessarily indicate
 * that the input string is valid JSON.
 *
 *
 * Each tokener may be used to parse a single JSON string. Instances of this
 * class are not thread safe. Although this class is nonfinal, it was not
 * designed for inheritance and should not be subclassed. In particular,
 * self-use by overridable methods is not specified. See *Effective Java*
 * Item 17, "Design and Document or inheritance or else prohibit it" for further
 * information.
 */
class JSONTokener
/**
 * @param `input` JSON encoded string. Null is not permitted and will yield a
 * tokener that throws `NullPointerExceptions` when methods are
 * called.
 */(
    /** The input JSON.  */
    private val input: String
) {
    /**
     * The index of the next character to be returned by [.next]. When
     * the input is exhausted, this equals the input's length.
     */
    private var pos = 0

    /**
     * Returns the next value from the input.
     *
     * @return a [org.jsonx.JSONObject], [org.jsonx.JSONArray], String, Boolean,
     * Integer, Long, Double or [org.jsonx.JSONObject.NULL_].
     * @throws org.jsonx.JSONException if the input is malformed.
     */
    @Throws(JSONException::class)
    fun nextValue(): Any {
        val c = nextCleanInternal()
        return when (c) {
            -1 -> throw syntaxError("End of input")
            '{'.toInt() -> readObject()
            '['.toInt() -> readArray()
            '\''.toInt(), '"'.toInt() -> nextString(c.toChar())
            else -> {
                pos--
                readLiteral()
            }
        }
    }

    @Throws(JSONException::class)
    private fun nextCleanInternal(): Int {
        while (pos < input.length) {
            val c = input[pos++].toInt()
            return when (c) {
                '\t'.toInt(), ' '.toInt(), '\n'.toInt(), '\r'.toInt() -> continue
                '/'.toInt() -> {
                    if (pos == input.length) {
                        return c
                    }
                    val peek = input[pos]
                    if (peek != '*' && peek != '/') {
                        return c
                    }
                    skipComment()
                    continue
                }
                else -> c
            }
        }
        return -1
    }

    /**
     * Advances the position until it is beyond the current comment. The opening
     * slash '/' should have already been read, and character at the current
     * position be an asterisk '*' for a C-style comment or a slash '/' for an
     * end-of-line comment.
     *
     * @throws org.jsonx.JSONException if a C-style comment was not terminated.
     */
    @Throws(JSONException::class)
    private fun skipComment() {
        if (input[pos++] == '*') {
            val commentEnd = input.indexOf("*/", pos)
            if (commentEnd == -1) {
                throw syntaxError("Unterminated comment")
            }
            pos = commentEnd + 2
        } else {
            /*
             * Skip to the next newline character. If the line is terminated by
             * "\r\n", the '\n' will be consumed as whitespace by the caller.
             */
            while (pos < input.length) {
                val c = input[pos]
                if (c == '\r' || c == '\n') {
                    pos++
                    break
                }
                pos++
            }
        }
    }

    /**
     * Returns the string up to but not including `quote`, unescaping any
     * character escape sequences encountered along the way. The opening quote
     * should have already been read. This consumes the closing quote, but does
     * not include it in the returned string.
     *
     * @param quote either ' or ".
     * @throws NumberFormatException if any unicode escape sequences are
     * malformed.
     */
    @Throws(JSONException::class)
    fun nextString(quote: Char): String {
        /*
         * For strings that are free of escape sequences, we can just extract
         * the result as a substring of the input. But if we encounter an escape
         * sequence, we need to use a StringBuilder to compose the result.
         */
        var builder: StringBuilder? = null
        /* the index of the first character not yet appended to the builder. */
        var start = pos
        while (pos < input.length) {
            val c = input[pos++].toInt()
            if (c == quote.toInt()) {
                return if (builder == null) {
                    // a new string avoids leaking memory
                    input.substring(start, pos - 1).toCharArray().concatToString()
                } else {
                    builder.append(input, start, pos - 1)
                    builder.toString()
                }
            }
            if (c == '\\'.toInt()) {
                if (pos == input.length) {
                    throw syntaxError("Unterminated escape sequence")
                }
                if (builder == null) {
                    builder = StringBuilder()
                }
                builder.append(input, start, pos - 1)
                builder.append(readEscapeCharacter())
                start = pos
            }
        }
        throw syntaxError("Unterminated string")
    }

    /**
     * Unescapes the character identified by the character or characters that
     * immediately follow a backslash. The backslash '\' should have already
     * been read. This supports both unicode escapes "u000A" and two-character
     * escapes "\n".
     *
     * @throws NumberFormatException if any unicode escape sequences are
     * malformed.
     */
    @Throws(JSONException::class)
    private fun readEscapeCharacter(): Char {
        val escaped = input[pos++]
        return when (escaped) {
            'u' -> {
                if (pos + 4 > input.length) {
                    throw syntaxError("Unterminated escape sequence")
                }
                val hex = input.substring(pos, pos + 4)
                pos += 4
                hex.toInt(16).toChar()
            }
            't' -> '\t'
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
//            'f' -> '\f'
            '\'', '"', '\\' -> escaped
            else -> escaped
        }
    }

    /**
     * Reads a null, boolean, numeric or unquoted string literal value. Numeric
     * values will be returned as an Integer, Long, or Double, in that order of
     * preference.
     */
    @Throws(JSONException::class)
    private fun readLiteral(): Any {
//        val literal = nextToInternal("{}[]/\\:,=;# \t\f")
        val literal = nextToInternal("{}[]/\\:,=;# \t")
        if (literal.length == 0) {
            throw syntaxError("Expected literal value")
        } else if ("null".equals(literal, ignoreCase = true)) {
            return JSONObject.NULL_
        } else if ("true".equals(literal, ignoreCase = true)) {
            return true
        } else if ("false".equals(literal, ignoreCase = true)) {
            return false
        }
        /* try to parse as an integral type... */if (literal.indexOf('.') == -1) {
            var base = 10
            var number = literal
            if (number.startsWith("0x") || number.startsWith("0X")) {
                number = number.substring(2)
                base = 16
            } else if (number.startsWith("0") && number.length > 1) {
                number = number.substring(1)
                base = 8
            }
            try {
                val longValue = number.toLong(base)
                return if (longValue <= Int.MAX_VALUE && longValue >= Int.MIN_VALUE) {
                    longValue.toInt()
                } else {
                    longValue
                }
            } catch (e: NumberFormatException) {
                /*
                 * This only happens for integral numbers greater than
                 * Long.MAX_VALUE, numbers in exponential form (5e-10) and
                 * unquoted strings. Fall through to try floating point.
                 */
            }
        }
        /* ...next try to parse as a floating point... */try {
            return literal.toDouble()
        } catch (e: NumberFormatException) {
        }
        /* ... finally give up. We have an unquoted string */return literal.toCharArray()
            .concatToString() // a new string avoids leaking memory
    }

    /**
     * Returns the string up to but not including any of the given characters or
     * a newline character. This does not consume the excluded character.
     */
    private fun nextToInternal(excluded: String): String {
        val start = pos
        while (pos < input.length) {
            val c = input[pos]
            if (c == '\r' || c == '\n' || excluded.indexOf(c) != -1) {
                return input.substring(start, pos)
            }
            pos++
        }
        return input.substring(start)
    }

    /**
     * Reads a sequence of key/value pairs and the trailing closing brace '}' of
     * an object. The opening brace '{' should have already been read.
     */
    @Throws(JSONException::class)
    private fun readObject(): JSONObject {
        val result = JSONObject()
        /* Peek to see if this is the empty object. */
        val first = nextCleanInternal()
        if (first == '}'.toInt()) {
            return result
        } else if (first != -1) {
            pos--
        }
        while (true) {
            val name = nextValue()
            if (name !is String) {
                if (name == null) {
                    throw syntaxError("Names cannot be null")
                } else {
                    throw syntaxError(
                        "Names must be strings"
                    )
                }
            }
            /*
             * Expect the name/value separator to be either a colon ':', an
             * equals sign '=', or an arrow "=>". The last two are bogus but we
             * include them because that's what the original implementation did.
             */
            val separator = nextCleanInternal()
            if (separator != ':'.toInt() && separator != '='.toInt()) {
                throw syntaxError("Expected ':' after $name")
            }
            if (pos < input.length && input[pos] == '>') {
                pos++
            }
            result.put(name, nextValue())
            return when (nextCleanInternal()) {
                '}'.toInt() -> result
                ';'.toInt(), ','.toInt() -> continue
                else -> throw syntaxError("Unterminated object")
            }
        }
    }

    /**
     * Reads a sequence of values and the trailing closing brace ']' of an
     * array. The opening brace '[' should have already been read. Note that
     * "[]" yields an empty array, but "[,]" returns a two-element array
     * equivalent to "[null,null]".
     */
    @Throws(JSONException::class)
    private fun readArray(): JSONArray {
        val result = JSONArray()
        /* to cover input that ends with ",]". */
        var hasTrailingSeparator = false
        while (true) {
            when (nextCleanInternal()) {
                -1 -> throw syntaxError("Unterminated array")
                ']'.toInt() -> {
                    if (hasTrailingSeparator) {
                        result.put(null)
                    }
                    return result
                }
                ','.toInt(), ';'.toInt() -> {
                    /* A separator without a value first means "null". */result.put(null)
                    hasTrailingSeparator = true
                    continue
                }
                else -> pos--
            }
            result.put(nextValue())
            return when (nextCleanInternal()) {
                ']'.toInt() -> result
                ','.toInt(), ';'.toInt() -> {
                    hasTrailingSeparator = true
                    continue
                }
                else -> throw syntaxError("Unterminated array")
            }
        }
    }

    /**
     * Returns an exception containing the given message plus the current
     * position and the entire input string.
     */
    fun syntaxError(message: String): JSONException {
        return JSONException(message + this)
    }

    /**
     * Returns the current position and the entire input string.
     */
    override fun toString(): String {
        // consistent with the original implementation
        return " at character $pos of $input"
    }
    /*
     * Legacy APIs.
     *
     * None of the methods below are on the critical path of parsing JSON
     * documents. They exist only because they were exposed by the original
     * implementation and may be used by some clients.
     */
    /**
     * Returns true until the input has been exhausted.
     */
    fun more(): Boolean {
        return pos < input.length
    }

    /**
     * Returns the next available character, or the null character '\0' if all
     * input has been exhausted. The return value of this method is ambiguous
     * for JSON strings that contain the character '\0'.
     */
    operator fun next(): Char {
        return if (pos < input.length) input[pos++] else '\u0000'
    }

    /**
     * Returns the next available character if it equals `c`. Otherwise an
     * exception is thrown.
     */
    @Throws(JSONException::class)
    fun next(c: Char): Char {
        val result = next()
        if (result != c) {
            throw syntaxError("Expected $c but was $result")
        }
        return result
    }

    /**
     * Returns the next character that is not whitespace and does not belong to
     * a comment. If the input is exhausted before such a character can be
     * found, the null character '\0' is returned. The return value of this
     * method is ambiguous for JSON strings that contain the character '\0'.
     */
    @Throws(JSONException::class)
    fun nextClean(): Char {
        val nextCleanInt = nextCleanInternal()
        return if (nextCleanInt == -1) '\u0000' else nextCleanInt.toChar()
    }

    /**
     * Returns the next `length` characters of the input.
     *
     *
     * The returned string shares its backing character array with this
     * tokener's input string. If a reference to the returned string may be held
     * indefinitely, you should use `new String(result)` to copy it first
     * to avoid memory leaks.
     *
     * @throws org.jsonx.JSONException if the remaining input is not long enough to
     * satisfy this request.
     */
    @Throws(JSONException::class)
    fun next(length: Int): String {
        if (pos + length > input.length) {
            throw syntaxError("$length is out of bounds")
        }
        val result = input.substring(pos, pos + length)
        pos += length
        return result
    }

    /**
     * Returns the [trimmed][String.trim] string holding the characters up
     * to but not including the first of:
     *
     *  * any character in `excluded`
     *  * a newline character '\n'
     *  * a carriage return '\r'
     *
     *
     *
     * The returned string shares its backing character array with this
     * tokener's input string. If a reference to the returned string may be held
     * indefinitely, you should use `new String(result)` to copy it first
     * to avoid memory leaks.
     *
     * @return a possibly-empty string
     */
    fun nextTo(excluded: String?): String {
        if (excluded == null) {
            throw NullPointerException()
        }
        return nextToInternal(excluded).trim { it <= ' ' }
    }

    /**
     * Equivalent to `nextTo(String.valueOf(excluded))`.
     */
    fun nextTo(excluded: Char): String {
        return nextToInternal(excluded.toString()).trim { it <= ' ' }
    }

    /**
     * Advances past all input up to and including the next occurrence of
     * `thru`. If the remaining input doesn't contain `thru`, the
     * input is exhausted.
     */
    fun skipPast(thru: String) {
        val thruStart = input.indexOf(thru, pos)
        pos = if (thruStart == -1) input.length else thruStart + thru.length
    }

    /**
     * Advances past all input up to but not including the next occurrence of
     * `to`. If the remaining input doesn't contain `to`, the input
     * is unchanged.
     */
    fun skipTo(to: Char): Char {
        val index = input.indexOf(to, pos)
        return if (index != -1) {
            pos = index
            to
        } else {
            '\u0000'
        }
    }

    /**
     * Unreads the most recent character of input. If no input characters have
     * been read, the input is unchanged.
     */
    fun back() {
        if (--pos == -1) {
            pos = 0
        }
    }

    companion object {
        /**
         * Returns the integer [0..15] value for the given hex character, or -1
         * for non-hex input.
         *
         * @param hex a character in the ranges [0-9], [A-F] or [a-f]. Any other
         * character will yield a -1 result.
         */
        fun dehexchar(hex: Char): Int {
            return if (hex >= '0' && hex <= '9') {
                hex - '0'
            } else if (hex >= 'A' && hex <= 'F') {
                hex - 'A' + 10
            } else if (hex >= 'a' && hex <= 'f') {
                hex - 'a' + 10
            } else {
                -1
            }
        }
    }
}