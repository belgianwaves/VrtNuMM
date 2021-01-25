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

import kotlin.jvm.JvmOverloads;

// Note: this class was written without inspecting the non-free org.json sourcecode.
/**
 * A modifiable set of name/value mappings. Names are unique, non-null strings.
 * Values may be any mix of [JSONObjects][JSONObject], [ JSONArrays][org.jsonx.JSONArray], Strings, Booleans, Integers, Longs, Doubles or [.NULL].
 * Values may not be `null`, [NaNs][Double.isNaN], [ ][Double.isInfinite], or of any type not listed here.
 *
 *
 * This class can coerce values to another type when requested.
 *
 *  * When the requested type is a boolean, strings will be coerced
 * using [Boolean.valueOf].
 *  * When the requested type is a double, other [Number] types will
 * be coerced using [doubleValue][Number.doubleValue]. Strings
 * that can be coerced using [Double.valueOf] will be.
 *  * When the requested type is an int, other [Number] types will
 * be coerced using [intValue][Number.intValue]. Strings
 * that can be coerced using [Double.valueOf] will be,
 * and then cast to int.
 *  * When the requested type is a long, other [Number] types will
 * be coerced using [longValue][Number.longValue]. Strings
 * that can be coerced using [Double.valueOf] will be,
 * and then cast to long. This two-step conversion is lossy for very
 * large values. For example, the string "9223372036854775806" yields the
 * long 9223372036854775807.
 *  * When the requested type is a String, other non-null values will be
 * coerced using [String.valueOf]. Although null cannot be
 * coerced, the sentinel value [JSONObject.NULL_] is coerced to the
 * string "null".
 *
 *
 *
 * This class can look up both mandatory and optional values:
 *
 *  * Use `get*Type*()` to retrieve a mandatory value. This
 * fails with a `JSONException` if the requested name has no value
 * or if the value cannot be coerced to the requested type.
 *  * Use `opt*Type*()` to retrieve an optional value. This
 * returns a system- or user-supplied default if the requested name has no
 * value or if the value cannot be coerced to the requested type.
 *
 *
 *
 * **Warning:** this class represents null in two incompatible
 * ways: the standard Java `null` reference, and the sentinel value [ ][JSONObject.NULL_]. In particular, calling `put(name, null)` removes the
 * named entry from the object but `put(name, JSONObject.NULL)` stores an
 * entry whose value is `JSONObject.NULL`.
 *
 *
 * Instances of this class are not thread safe. Although this class is
 * nonfinal, it was not designed for inheritance and should not be subclassed.
 * In particular, self-use by overridable methods is not specified. See
 * *Effective Java* Item 17, "Design and Document or inheritance or else
 * prohibit it" for further information.
 */
class JSONObject {
    private val nameValuePairs: MutableMap<String?, Any>

    /**
     * Creates a `JSONObject` with no name/value mappings.
     */
    constructor() {
        nameValuePairs = HashMap()
    }

    /**
     * Creates a new `JSONObject` by copying all name/value mappings from
     * the given map.
     *
     * @param copyFrom a map whose keys are of type [String] and whose
     * values are of supported types.
     * @throws NullPointerException if any of the map's keys are null.
     */
    /* (accept a raw type for API compatibility) */
    constructor(copyFrom: Map<*, *>) : this() {
        for ((key1, value) in copyFrom) {
            /*
             * Deviate from the original by checking that keys are non-null and
             * of the proper type. (We still defer validating the values).
             */
            val key = key1 as String ?: throw NullPointerException()
            nameValuePairs[key] = value as Any
        }
    }

    /**
     * Creates a new `JSONObject` with name/value mappings from the next
     * object in the tokener.
     *
     * @param readFrom a tokener whose nextValue() method will yield a
     * `JSONObject`.
     * @throws org.jsonx.JSONException if the parse fails or doesn't yield a
     * `JSONObject`.
     */
    constructor(readFrom: JSONTokener) {
        /*
         * Getting the parser to populate this could get tricky. Instead, just
         * parse to temporary JSONObject and then steal the data from that.
         */
        val `object` = readFrom.nextValue()
        if (`object` is JSONObject) {
            nameValuePairs = `object`.nameValuePairs
        } else {
            throw JSON.typeMismatch(`object`, "JSONObject")
        }
    }

    /**
     * Creates a new `JSONObject` with name/value mappings from the JSON
     * string.
     *
     * @param json a JSON-encoded string containing an object.
     * @throws org.jsonx.JSONException if the parse fails or doesn't yield a `JSONObject`.
     */
    constructor(json: String) : this(JSONTokener(json)) {}

    /**
     * Creates a new `JSONObject` by copying mappings for the listed names
     * from the given object. Names that aren't present in `copyFrom` will
     * be skipped.
     */
    constructor(copyFrom: JSONObject, names: Array<String?>) : this() {
        for (name in names) {
            val value = copyFrom.opt(name)
            if (value != null) {
                nameValuePairs[name] = value
            }
        }
    }

    /**
     * Returns the number of name/value mappings in this object.
     */
    fun length(): Int {
        return nameValuePairs.size
    }

    /**
     * Maps `name` to `value`, clobbering any existing name/value
     * mapping with the same name.
     *
     * @return this object.
     */
    
    fun put(name: String?, value: Boolean): JSONObject {
        nameValuePairs[checkName(name)] = value
        return this
    }

    /**
     * Maps `name` to `value`, clobbering any existing name/value
     * mapping with the same name.
     *
     * @param value a finite value. May not be [NaNs][Double.isNaN] or
     * [infinities][Double.isInfinite].
     * @return this object.
     */
    
    fun put(name: String?, value: Double): JSONObject {
        nameValuePairs[checkName(name)] = JSON.checkDouble(value)
        return this
    }

    /**
     * Maps `name` to `value`, clobbering any existing name/value
     * mapping with the same name.
     *
     * @return this object.
     */
    
    fun put(name: String?, value: Int): JSONObject {
        nameValuePairs[checkName(name)] = value
        return this
    }

    /**
     * Maps `name` to `value`, clobbering any existing name/value
     * mapping with the same name.
     *
     * @return this object.
     */
    
    fun put(name: String?, value: Long): JSONObject {
        nameValuePairs[checkName(name)] = value
        return this
    }

    /**
     * Maps `name` to `value`, clobbering any existing name/value
     * mapping with the same name. If the value is `null`, any existing
     * mapping for `name` is removed.
     *
     * @param value a [JSONObject], [org.jsonx.JSONArray], String, Boolean,
     * Integer, Long, Double, [.NULL], or `null`. May not be
     * [NaNs][Double.isNaN] or [     infinities][Double.isInfinite].
     * @return this object.
     */
    
    fun put(name: String?, value: Any?): JSONObject {
        if (value == null) {
            nameValuePairs.remove(name)
            return this
        }
        if (value is Number) {
            // deviate from the original by checking all Numbers, not just floats & doubles
            JSON.checkDouble(value.toDouble())
        }
        nameValuePairs[checkName(name)] = value
        return this
    }

    /**
     * Equivalent to `put(name, value)` when both parameters are non-null;
     * does nothing otherwise.
     */
    
    fun putOpt(name: String?, value: Any?): JSONObject {
        return if (name == null || value == null) {
            this
        } else put(name, value)
    }

    /**
     * Appends `value` to the array already mapped to `name`. If
     * this object has no mapping for `name`, this inserts a new mapping.
     * If the mapping exists but its value is not an array, the existing
     * and new values are inserted in order into a new array which is itself
     * mapped to `name`. In aggregate, this allows values to be added to a
     * mapping one at a time.
     *
     * @param value a [JSONObject], [org.jsonx.JSONArray], String, Boolean,
     * Integer, Long, Double, [.NULL] or null. May not be [     ][Double.isNaN] or [infinities][Double.isInfinite].
     */
    
    fun accumulate(name: String?, value: Any?): JSONObject {
        val current = nameValuePairs[checkName(name)] ?: return put(name, value)
        // check in accumulate, since array.put(Object) doesn't do any checking
        if (value is Number) {
            JSON.checkDouble(value.toDouble())
        }
        if (current is JSONArray) {
            current.put(value)
        } else {
            val array = JSONArray()
            array.put(current)
            array.put(value)
            nameValuePairs[name] = array
        }
        return this
    }

    
    fun checkName(name: String?): String {
        if (name == null) {
            throw JSONException("Names must be non-null")
        }
        return name
    }

    /**
     * Removes the named mapping if it exists; does nothing otherwise.
     *
     * @return the value previously mapped by `name`, or null if there was
     * no such mapping.
     */
    fun remove(name: String?): Any? {
        return nameValuePairs.remove(name)
    }

    /**
     * Returns true if this object has no mapping for `name` or if it has
     * a mapping whose value is [.NULL].
     */
    fun isNull(name: String?): Boolean {
        val value = nameValuePairs[name]
        return value == null || value === NULL_
    }

    /**
     * Returns true if this object has a mapping for `name`. The mapping
     * may be [.NULL].
     */
    fun has(name: String?): Boolean {
        return nameValuePairs.containsKey(name)
    }

    /**
     * Returns the value mapped by `name`.
     *
     * @throws org.jsonx.JSONException if no such mapping exists.
     */
    
    operator fun get(name: String): Any {
        return nameValuePairs[name] ?: throw JSONException("No value for $name")
    }

    /**
     * Returns the value mapped by `name`, or null if no such mapping
     * exists.
     */
    fun opt(name: String?): Any? {
        return nameValuePairs[name]
    }

    /**
     * Returns the value mapped by `name` if it exists and is a boolean or
     * can be coerced to a boolean.
     *
     * @throws org.jsonx.JSONException if the mapping doesn't exist or cannot be coerced
     * to a boolean.
     */
    
    fun getBoolean(name: String): Boolean {
        val `object` = get(name)
        return JSON.toBoolean(`object`) ?: throw JSON.typeMismatch(name, `object`, "boolean")
    }
    /**
     * Returns the value mapped by `name` if it exists and is a boolean or
     * can be coerced to a boolean. Returns `fallback` otherwise.
     */
    /**
     * Returns the value mapped by `name` if it exists and is a boolean or
     * can be coerced to a boolean. Returns false otherwise.
     */
    @JvmOverloads
    fun optBoolean(name: String?, fallback: Boolean = false): Boolean {
        val `object` = opt(name)
        val result = JSON.toBoolean(`object`)
        return result ?: fallback
    }

    /**
     * Returns the value mapped by `name` if it exists and is a double or
     * can be coerced to a double.
     *
     * @throws org.jsonx.JSONException if the mapping doesn't exist or cannot be coerced
     * to a double.
     */
    
    fun getDouble(name: String): Double {
        val `object` = get(name)
        return JSON.toDouble(`object`) ?: throw JSON.typeMismatch(name, `object`, "double")
    }
    /**
     * Returns the value mapped by `name` if it exists and is a double or
     * can be coerced to a double. Returns `fallback` otherwise.
     */
    /**
     * Returns the value mapped by `name` if it exists and is a double or
     * can be coerced to a double. Returns `NaN` otherwise.
     */
    @JvmOverloads
    fun optDouble(name: String?, fallback: Double = Double.NaN): Double {
        val `object` = opt(name)
        val result = JSON.toDouble(`object`)
        return result ?: fallback
    }

    /**
     * Returns the value mapped by `name` if it exists and is an int or
     * can be coerced to an int.
     *
     * @throws org.jsonx.JSONException if the mapping doesn't exist or cannot be coerced
     * to an int.
     */
    
    fun getInt(name: String): Int {
        val `object` = get(name)
        return JSON.toInteger(`object`) ?: throw JSON.typeMismatch(name, `object`, "int")
    }
    /**
     * Returns the value mapped by `name` if it exists and is an int or
     * can be coerced to an int. Returns `fallback` otherwise.
     */
    /**
     * Returns the value mapped by `name` if it exists and is an int or
     * can be coerced to an int. Returns 0 otherwise.
     */
    @JvmOverloads
    fun optInt(name: String?, fallback: Int = 0): Int {
        val `object` = opt(name)
        val result = JSON.toInteger(`object`)
        return result ?: fallback
    }

    /**
     * Returns the value mapped by `name` if it exists and is a long or
     * can be coerced to a long.
     *
     * @throws org.jsonx.JSONException if the mapping doesn't exist or cannot be coerced
     * to a long.
     */
    
    fun getLong(name: String): Long {
        val `object` = get(name)
        return JSON.toLong(`object`) ?: throw JSON.typeMismatch(name, `object`, "long")
    }
    /**
     * Returns the value mapped by `name` if it exists and is a long or
     * can be coerced to a long. Returns `fallback` otherwise.
     */
    /**
     * Returns the value mapped by `name` if it exists and is a long or
     * can be coerced to a long. Returns 0 otherwise.
     */
    @JvmOverloads
    fun optLong(name: String?, fallback: Long = 0L): Long {
        val `object` = opt(name)
        val result = JSON.toLong(`object`)
        return result ?: fallback
    }

    /**
     * Returns the value mapped by `name` if it exists, coercing it if
     * necessary.
     *
     * @throws org.jsonx.JSONException if no such mapping exists.
     */
    
    fun getString(name: String): String {
        val `object` = get(name)
        return JSON.toString(`object`) ?: throw JSON.typeMismatch(name, `object`, "String")
    }
    /**
     * Returns the value mapped by `name` if it exists, coercing it if
     * necessary. Returns `fallback` if no such mapping exists.
     */
    /**
     * Returns the value mapped by `name` if it exists, coercing it if
     * necessary. Returns the empty string if no such mapping exists.
     */
    @JvmOverloads
    fun optString(name: String?, fallback: String? = ""): String {
        val `object` = opt(name)
        val result = JSON.toString(`object`)
        return result ?: fallback!!
    }

    /**
     * Returns the value mapped by `name` if it exists and is a `JSONArray`.
     *
     * @throws org.jsonx.JSONException if the mapping doesn't exist or is not a `JSONArray`.
     */
    
    fun getJSONArray(name: String): JSONArray {
        val `object` = get(name)
        return if (`object` is JSONArray) {
            `object`
        } else {
            throw JSON.typeMismatch(name, `object`, "JSONArray")
        }
    }

    /**
     * Returns the value mapped by `name` if it exists and is a `JSONArray`. Returns null otherwise.
     */
    fun optJSONArray(name: String?): JSONArray? {
        val `object` = opt(name)
        return if (`object` is JSONArray) `object` else null
    }

    /**
     * Returns the value mapped by `name` if it exists and is a `JSONObject`.
     *
     * @throws org.jsonx.JSONException if the mapping doesn't exist or is not a `JSONObject`.
     */
    
    fun getJSONObject(name: String): JSONObject {
        val `object` = get(name)
        return if (`object` is JSONObject) {
            `object`
        } else {
            throw JSON.typeMismatch(name, `object`, "JSONObject")
        }
    }

    /**
     * Returns the value mapped by `name` if it exists and is a `JSONObject`. Returns null otherwise.
     */
    fun optJSONObject(name: String?): JSONObject? {
        val `object` = opt(name)
        return if (`object` is JSONObject) `object` else null
    }

    /**
     * Returns an array with the values corresponding to `names`. The
     * array contains null for names that aren't mapped. This method returns
     * null if `names` is either null or empty.
     */
    
    fun toJSONArray(names: JSONArray?): JSONArray? {
        val result = JSONArray()
        if (names == null) {
            return null
        }
        val length = names.length()
        if (length == 0) {
            return null
        }
        for (i in 0 until length) {
            val name = JSON.toString(names.opt(i))
            result.put(opt(name))
        }
        return result
    }

    /**
     * Returns an iterator of the `String` names in this object. The
     * returned iterator supports [remove][Iterator.remove], which will
     * remove the corresponding mapping from this object. If this object is
     * modified after the iterator is returned, the iterator's behavior is
     * undefined. The order of the keys is undefined.
     */
    /* Return a raw type for API compatibility */
    fun keys(): Iterator<*> {
        return nameValuePairs.keys.iterator()
    }

    /**
     * Returns an array containing the string names in this object. This method
     * returns null if this object contains no mappings.
     */
    fun names(): JSONArray? {
        return if (nameValuePairs.isEmpty()) null else JSONArray(ArrayList(nameValuePairs.keys))
    }

    /**
     * Encodes this object as a compact JSON string, such as:
     * <pre>{"query":"Pizza","locations":[94043,90210]}</pre>
     */
    override fun toString(): String {
        return try {
            val stringer = JSONStringer()
            writeTo(stringer)
            stringer.toString()
        } catch (e: JSONException) {
            ""
        }
    }

    /**
     * Encodes this object as a human readable JSON string for debugging, such
     * as:
     * <pre>
     * {
     * "query": "Pizza",
     * "locations": [
     * 94043,
     * 90210
     * ]
     * }</pre>
     *
     * @param indentSpaces the number of spaces to indent for each level of
     * nesting.
     */
    
    fun toString(indentSpaces: Int): String {
        val stringer = JSONStringer(indentSpaces)
        writeTo(stringer)
        return stringer.toString()
    }

    
    fun writeTo(stringer: JSONStringer) {
        stringer.`object`()
        for ((key, value) in nameValuePairs) {
            stringer.key(key).value(value)
        }
        stringer.endObject()
    }

    companion object {
        private const val NEGATIVE_ZERO = -0.0

        /**
         * A sentinel value used to explicitly define a name with no value. Unlike
         * `null`, names with this value:
         *
         *  * show up in the [.names] array
         *  * show up in the [.keys] iterator
         *  * return `true` for [.has]
         *  * do not throw on [.get]
         *  * are included in the encoded JSON string.
         *
         *
         *
         * This value violates the general contract of [Object.equals] by
         * returning true when compared to `null`. Its [.toString]
         * method returns "null".
         */
        val NULL_: Any = object : Any() {
            override fun equals(o: Any?): Boolean {
                return o === this || o == null // API specifies this broken equals implementation
            }

            override fun toString(): String {
                return "null"
            }
        }

        /**
         * Encodes the number as a JSON string.
         *
         * @param number a finite value. May not be [NaNs][Double.isNaN] or
         * [infinities][Double.isInfinite].
         */
        
        fun numberToString(number: Number?): String {
            if (number == null) {
                throw JSONException("Number must be non-null")
            }
            val doubleValue = number.toDouble()
            JSON.checkDouble(doubleValue)
            // the original returns "-0" instead of "-0.0" for negative zero
            if (number == NEGATIVE_ZERO) {
                return "-0"
            }
            val longValue = number.toLong()
            return if (doubleValue == longValue.toDouble()) {
                longValue.toString()
            } else number.toString()
        }

        /**
         * Encodes `data` as a JSON string. This applies quotes and any
         * necessary character escaping.
         *
         * @param data the string to encode. Null will be interpreted as an empty
         * string.
         */
        fun quote(data: String?): String {
            return if (data == null) {
                "\"\""
            } else try {
                val stringer = JSONStringer()
                stringer.open(JSONStringer.Scope.NULL, "")
                stringer.value(data)
                stringer.close(JSONStringer.Scope.NULL, JSONStringer.Scope.NULL, "")
                stringer.toString()
            } catch (e: JSONException) {
                throw AssertionError()
            }
        }
    }
}