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
 * Implements [org.jsonx.JSONObject.toString] and [org.jsonx.JSONArray.toString]. Most
 * application developers should use those methods directly and disregard this
 * API. For example:<pre>
 * JSONObject object = ...
 * String json = object.toString();</pre>
 *
 *
 * Stringers only encode well-formed JSON strings. In particular:
 *
 *  * The stringer must have exactly one top-level array or object.
 *  * Lexical scopes must be balanced: every call to [.array] must
 * have a matching call to [.endArray] and every call to [       ][.object] must have a matching call to [.endObject].
 *  * Arrays may not contain keys (property names).
 *  * Objects must alternate keys (property names) and values.
 *  * Values are inserted with either literal [value][.value]
 * calls, or by nesting arrays or objects.
 *
 * Calls that would result in a malformed JSON string will fail with a
 * [org.jsonx.JSONException].
 *
 *
 * This class provides no facility for pretty-printing (ie. indenting)
 * output. To encode indented output, use [org.jsonx.JSONObject.toString] or
 * [org.jsonx.JSONArray.toString].
 *
 *
 * Some implementations of the API support at most 20 levels of nesting.
 * Attempts to create more than 20 levels of nesting may fail with a [ ].
 *
 *
 * Each stringer may be used to encode a single top level value. Instances of
 * this class are not thread safe. Although this class is nonfinal, it was not
 * designed for inheritance and should not be subclassed. In particular,
 * self-use by overridable methods is not specified. See *Effective Java*
 * Item 17, "Design and Document or inheritance or else prohibit it" for further
 * information.
 */
class JSONStringer {
    /** The output data, containing at most one top-level array or object.  */
    val out = StringBuilder()

    /**
     * Lexical scoping elements within this stringer, necessary to insert the
     * appropriate separator characters (ie. commas and colons) and to detect
     * nesting errors.
     */
    enum class Scope {
        /**
         * An array with no elements requires no separators or newlines before
         * it is closed.
         */
        EMPTY_ARRAY,

        /**
         * A array with at least one value requires a comma and newline before
         * the next element.
         */
        NONEMPTY_ARRAY,

        /**
         * An object with no keys or values requires no separators or newlines
         * before it is closed.
         */
        EMPTY_OBJECT,

        /**
         * An object whose most recent element is a key. The next element must
         * be a value.
         */
        DANGLING_KEY,

        /**
         * An object with at least one name/value pair requires a comma and
         * newline before the next element.
         */
        NONEMPTY_OBJECT,

        /**
         * A special bracketless array needed by JSONStringer.join() and
         * JSONObject.quote() only. Not used for JSON encoding.
         */
        NULL
    }

    /**
     * Unlike the original implementation, this stack isn't limited to 20
     * levels of nesting.
     */
    private val stack: MutableList<Scope> = ArrayList()

    /**
     * A string containing a full set of spaces for a single level of
     * indentation, or null for no pretty printing.
     */
    private val indent: String?

    constructor() {
        indent = null
    }

    internal constructor(indentSpaces: Int) {
        val indentChars = CharArray(indentSpaces) { _ -> ' ' }
        indent = indentChars.concatToString()
    }

    /**
     * Begins encoding a new array. Each call to this method must be paired with
     * a call to [.endArray].
     *
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun array(): JSONStringer {
        return open(Scope.EMPTY_ARRAY, "[")
    }

    /**
     * Ends encoding the current array.
     *
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun endArray(): JSONStringer {
        return close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]")
    }

    /**
     * Begins encoding a new object. Each call to this method must be paired
     * with a call to [.endObject].
     *
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun `object`(): JSONStringer {
        return open(Scope.EMPTY_OBJECT, "{")
    }

    /**
     * Ends encoding the current object.
     *
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun endObject(): JSONStringer {
        return close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}")
    }

    /**
     * Enters a new scope by appending any necessary whitespace and the given
     * bracket.
     */
    @Throws(JSONException::class)
    fun open(empty: Scope, openBracket: String?): JSONStringer {
        if (stack.isEmpty() && out.length > 0) {
            throw JSONException("Nesting problem: multiple top-level roots")
        }
        beforeValue()
        stack.add(empty)
        out.append(openBracket)
        return this
    }

    /**
     * Closes the current scope by appending any necessary whitespace and the
     * given bracket.
     */
    @Throws(JSONException::class)
    fun close(empty: Scope, nonempty: Scope, closeBracket: String?): JSONStringer {
        val context = peek()
        if (context != nonempty && context != empty) {
            throw JSONException("Nesting problem")
        }
        stack.removeAt(stack.size - 1)
        if (context == nonempty) {
            newline()
        }
        out.append(closeBracket)
        return this
    }

    /**
     * Returns the value on the top of the stack.
     */
    @Throws(JSONException::class)
    private fun peek(): Scope {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        return stack[stack.size - 1]
    }

    /**
     * Replace the value on the top of the stack with the given value.
     */
    private fun replaceTop(topOfStack: Scope) {
        stack[stack.size - 1] = topOfStack
    }

    /**
     * Encodes `value`.
     *
     * @param value a [org.jsonx.JSONObject], [org.jsonx.JSONArray], String, Boolean,
     * Integer, Long, Double or null. May not be [NaNs][Double.isNaN]
     * or [infinities][Double.isInfinite].
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun value(value: Any?): JSONStringer {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        if (value is JSONArray) {
            // ((JSONArray) value).writeTo(this);
            return this
        } else if (value is JSONObject) {
            // ((JSONObject) value).writeTo(this);
            return this
        }
        beforeValue()
        if (value == null || value is Boolean
            || value === JSONObject.NULL_
        ) {
            out.append(value)
        } else if (value is Number) {
            out.append(JSONObject.numberToString(value as Number?))
        } else {
            string(value.toString())
        }
        return this
    }

    /**
     * Encodes `value` to this stringer.
     *
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun value(value: Boolean): JSONStringer {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        beforeValue()
        out.append(value)
        return this
    }

    /**
     * Encodes `value` to this stringer.
     *
     * @param value a finite value. May not be [NaNs][Double.isNaN] or
     * [infinities][Double.isInfinite].
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun value(value: Double): JSONStringer {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        beforeValue()
        out.append(JSONObject.numberToString(value))
        return this
    }

    /**
     * Encodes `value` to this stringer.
     *
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun value(value: Long): JSONStringer {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        beforeValue()
        out.append(value)
        return this
    }

    private fun string(value: String) {
        out.append("\"")
        var i = 0
        val length = value.length
        while (i < length) {
            val c = value[i]
            when (c) {
                '"', '\\', '/' -> out.append('\\').append(c)
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
//                '\f' -> out.append("\\f")
                else -> if (c.toInt() <= 0x1F) {
//                    out.append(String.format("\\u%04x", c.toInt()))
                    out.append(c)
                } else {
                    out.append(c)
                }
            }
            i++
        }
        out.append("\"")
    }

    private fun newline() {
        if (indent == null) {
            return
        }
        out.append("\n")
        for (i in stack.indices) {
            out.append(indent)
        }
    }

    /**
     * Encodes the key (property name) to this stringer.
     *
     * @param name the name of the forthcoming value. May not be null.
     * @return this stringer.
     */
    @Throws(JSONException::class)
    fun key(name: String?): JSONStringer {
        if (name == null) {
            throw JSONException("Names must be non-null")
        }
        beforeKey()
        string(name)
        return this
    }

    /**
     * Inserts any necessary separators and whitespace before a name. Also
     * adjusts the stack to expect the key's value.
     */
    @Throws(JSONException::class)
    private fun beforeKey() {
        val context = peek()
        if (context == JSONStringer.Scope.NONEMPTY_OBJECT) { // first in object
            out.append(',')
        } else if (context != JSONStringer.Scope.EMPTY_OBJECT) { // not in an object!
            throw JSONException("Nesting problem")
        }
        newline()
        replaceTop(JSONStringer.Scope.DANGLING_KEY)
    }

    /**
     * Inserts any necessary separators and whitespace before a literal value,
     * inline array, or inline object. Also adjusts the stack to expect either a
     * closing bracket or another element.
     */
    @Throws(JSONException::class)
    private fun beforeValue() {
        if (stack.isEmpty()) {
            return
        }
        val context = peek()
        if (context == Scope.EMPTY_ARRAY) { // first in array
            replaceTop(Scope.NONEMPTY_ARRAY)
            newline()
        } else if (context == Scope.NONEMPTY_ARRAY) { // another in array
            out.append(',')
            newline()
        } else if (context == Scope.DANGLING_KEY) { // value for key
            out.append(if (indent == null) ":" else ": ")
            replaceTop(Scope.NONEMPTY_OBJECT)
        } else if (context != Scope.NULL) {
            throw JSONException("Nesting problem")
        }
    }

    /**
     * Returns the encoded JSON string.
     *
     *
     * If invoked with unterminated arrays or unclosed objects, this method's
     * return value is undefined.
     *
     *
     * **Warning:** although it contradicts the general contract
     * of [Object.toString], this method returns null if the stringer
     * contains no data.
     */
    override fun toString(): String {
        return if (out.length == 0) "" else out.toString()
    }
}