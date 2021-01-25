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

internal object JSON {
    /**
     * Returns the input if it is a JSON-permissable value; throws otherwise.
     */
    
    fun checkDouble(d: Double): Double {
        if (d.isInfinite() || d.isNaN()) {
            throw JSONException("Forbidden numeric value: $d")
        }
        return d
    }

    fun toBoolean(value: Any?): Boolean? {
        return if (value is Boolean) {
            value
        } else if (value is String) {
            value.toBoolean()
        } else {
            null
        }
    }

    fun toDouble(value: Any?): Double? {
        if (value is Double) {
            return value
        } else if (value is Number) {
            return value.toDouble()
        } else if (value is String) {
            try {
                return value.toDouble()
            } catch (e: NumberFormatException) {
            }
        }
        return null
    }

    fun toInteger(value: Any?): Int? {
        if (value is Int) {
            return value
        } else if (value is Number) {
            return value.toInt()
        } else if (value is String) {
            try {
                return value.toDouble().toInt()
            } catch (e: NumberFormatException) {
            }
        }
        return null
    }

    fun toLong(value: Any?): Long? {
        if (value is Long) {
            return value
        } else if (value is Number) {
            return value.toLong()
        } else if (value is String) {
            try {
                return value.toDouble().toLong()
            } catch (e: NumberFormatException) {
            }
        }
        return null
    }

    fun toString(value: Any?): String? {
        if (value is String) {
            return value
        } else if (value != null) {
            return value.toString()
        }
        return null
    }

    
    fun typeMismatch(
        indexOrName: Any, actual: Any?,
        requiredType: String
    ): JSONException {
        if (actual == null) {
            throw JSONException("Value at $indexOrName is null.")
        } else {
            throw JSONException(
                "Value " + actual + " at " + indexOrName
                        + " cannot be converted to " + requiredType
            )
        }
    }

    
    fun typeMismatch(actual: Any?, requiredType: String): JSONException {
        if (actual == null) {
            throw JSONException("Value is null.")
        } else {
            throw JSONException(
                "Value " + actual
                        + " cannot be converted to " + requiredType
            )
        }
    }
}