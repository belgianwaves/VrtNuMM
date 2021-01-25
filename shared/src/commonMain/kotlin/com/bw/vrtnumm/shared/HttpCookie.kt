package com.bw.vrtnumm.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

/**
 * An opaque key-value value pair held by an HTTP client to permit a stateful
 * session with an HTTP server. This class parses cookie headers for all three
 * commonly used HTTP cookie specifications:
 *
 *
 *  * The Netscape cookie spec is officially obsolete but widely used in
 * practice. Each cookie contains one key-value pair and the following
 * attributes: `Domain`, `Expires`, `Path`, and
 * `Secure`. The [version][.getVersion] of cookies in this
 * format is `0`.
 *
 * There are no accessors for the `Expires` attribute. When
 * parsed, expires attributes are assigned to the [         Max-Age][.getMaxAge] attribute as an offset from [         now][System.currentTimeMillis].
 *  * [RFC 2109](http://www.ietf.org/rfc/rfc2109.txt) formalizes
 * the Netscape cookie spec. It replaces the `Expires` timestamp
 * with a `Max-Age` duration and adds `Comment` and `Version` attributes. The [version][.getVersion] of cookies in
 * this format is `1`.
 *  * [RFC 2965](http://www.ietf.org/rfc/rfc2965.txt) refines
 * RFC 2109. It adds `Discard`, `Port`, and `CommentURL` attributes and renames the header from `Set-Cookie`
 * to `Set-Cookie2`. The [version][.getVersion] of cookies
 * in this format is `1`.
 *
 *
 *
 * This implementation silently discards unrecognized attributes. In
 * particular, the `HttpOnly` attribute is widely served but isn't in any
 * of the above specs. It was introduced by Internet Explorer to prevent server
 * cookies from being exposed in the DOM to JavaScript, etc.
 *
 * @since 1.6
 */
class HttpCookie(name: String, value: String)  {
    companion object {
        private val RESERVED_NAMES: MutableSet<String> = HashSet()

        /**
         * Returns true if `host` matches the domain pattern `domain`.
         *
         * @param domainPattern a host name (like `android.com` or `localhost`), or a pattern to match subdomains of a domain name (like
         * `.android.com`). A special case pattern is `.local`,
         * which matches all hosts without a TLD (like `localhost`).
         * @param host the host name or IP address from an HTTP request.
         */
        fun domainMatches(domainPattern: String?, host: String?): Boolean {
            if (domainPattern == null || host == null) {
                return false
            }
            val a = host.toLowerCase()
            val b = domainPattern.toLowerCase()
            /*
         * From the spec: "both host names are IP addresses and their host name strings match
         * exactly; or both host names are FQDN strings and their host name strings match exactly"
         */if (a == b && isFullyQualifiedDomainName(a, 0)) {
                return true
            }
            if (!isFullyQualifiedDomainName(a, 0)) {
                return b == ".local"
            }
            /*
         * Not in the spec! If prefixing a hostname with "." causes it to equal the domain pattern,
         * then it should match. This is necessary so that the pattern ".google.com" will match the
         * host "google.com".
         */return if (b.length == 1 + a.length && b.startsWith(".")
                && b.endsWith(a)
                && isFullyQualifiedDomainName(b, 1)
            ) {
                true
            } else a.length > b.length && a.endsWith(b)
                    && (b.startsWith(".") && isFullyQualifiedDomainName(
                b,
                1
            ) || b == ".local")
            /*
         * From the spec: "A is a HDN string and has the form NB, where N is a
         * non-empty name string, B has the form .B', and B' is a HDN string.
         * (So, x.y.com domain-matches .Y.com but not Y.com.)
         */
        }


        /**
         * Returns true if `s.substring(firstCharacter)` contains a dot
         * between its first and last characters, exclusive. This considers both
         * `android.com` and `co.uk` to be fully qualified domain names,
         * but not `android.com.`, `.com`. or `android`.
         *
         *
         * Although this implements the cookie spec's definition of FQDN, it is
         * not general purpose. For example, this returns true for IPv4 addresses.
         */
        private fun isFullyQualifiedDomainName(s: String, firstCharacter: Int): Boolean {
            val dotPosition = s.indexOf('.', firstCharacter + 1)
            return dotPosition != -1 && dotPosition < s.length - 1
        }

        /**
         * Constructs a cookie from a string. The string should comply with
         * set-cookie or set-cookie2 header format as specified in RFC 2965. Since
         * set-cookies2 syntax allows more than one cookie definitions in one
         * header, the returned object is a list.
         *
         * @param header
         * a set-cookie or set-cookie2 header.
         * @return a list of constructed cookies
         * @throws IllegalArgumentException
         * if the string does not comply with cookie specification, or
         * the cookie name contains illegal characters, or reserved
         * tokens of cookie specification appears
         * @throws NullPointerException
         * if header is null
         */
        @ExperimentalTime
        fun parse(header: String): List<HttpCookie> {
            return CookieParser(header).parse()
        }

        init {
            RESERVED_NAMES.add("comment") //           RFC 2109  RFC 2965
            RESERVED_NAMES.add("commenturl") //                     RFC 2965
            RESERVED_NAMES.add("discard") //                     RFC 2965
            RESERVED_NAMES.add("domain") // Netscape  RFC 2109  RFC 2965
            RESERVED_NAMES.add("expires") // Netscape
            RESERVED_NAMES.add("max-age") //           RFC 2109  RFC 2965
            RESERVED_NAMES.add("path") // Netscape  RFC 2109  RFC 2965
            RESERVED_NAMES.add("port") //                     RFC 2965
            RESERVED_NAMES.add("secure") // Netscape  RFC 2109  RFC 2965
            RESERVED_NAMES.add("version") //           RFC 2109  RFC 2965
        }
    }

    internal class CookieParser(private val input: String) {
        private val inputLowerCase: String
        private var pos = 0

        /*
         * The cookie's version is set based on an overly complex heuristic:
         * If it has an expires attribute, the version is 0.
         * Otherwise, if it has a max-age attribute, the version is 1.
         * Otherwise, if the cookie started with "Set-Cookie2", the version is 1.
         * Otherwise, if it has any explicit version attributes, use the first one.
         * Otherwise, the version is 0.
         */
        var hasExpires = false
        var hasMaxAge = false
        var hasVersion = false
        @ExperimentalTime
        fun parse(): List<HttpCookie> {
            val cookies: MutableList<HttpCookie> = ArrayList(2)
            // The RI permits input without either the "Set-Cookie:" or "Set-Cookie2" headers.
            var pre2965 = true
            if (inputLowerCase.startsWith("set-cookie2:")) {
                pos += "set-cookie2:".length
                pre2965 = false
                hasVersion = true
            } else if (inputLowerCase.startsWith("set-cookie:")) {
                pos += "set-cookie:".length
            }
            /*
             * Read a comma-separated list of cookies. Note that the values may contain commas!
             *   <NAME> "=" <VALUE> ( ";" <ATTR NAME> ( "=" <ATTR VALUE> )? )*
             */while (true) {
                val name = readAttributeName(false)
                if (name == null) {
                    require(!cookies.isEmpty()) { "No cookies in $input" }
                    return cookies
                }
                require(readEqualsSign()) { "Expected '=' after $name in $input" }
                val value = readAttributeValue(if (pre2965) ";" else ",;")
                val cookie = HttpCookie(name, value)
                cookie.version = if (pre2965) 0 else 1
                cookies.add(cookie)
                /*
                 * Read the attributes of the current cookie. Each iteration of this loop should
                 * enter with input either exhausted or prefixed with ';' or ',' as in ";path=/"
                 * and ",COOKIE2=value2".
                 */while (true) {
                    skipWhitespace()
                    if (pos == input.length) {
                        break
                    }
                    if (input[pos] == ',') {
                        pos++
                        break // a true comma delimiter; the current cookie is complete.
                    } else if (input[pos] == ';') {
                        pos++
                    }
                    val attributeName = readAttributeName(true)
                        ?: continue  // for empty attribute as in "Set-Cookie: foo=Foo;;path=/"
                    /*
                     * Since expires and port attributes commonly include comma delimiters, always
                     * scan until a semicolon when parsing these attributes.
                     */
                    val terminators = if (pre2965
                        || "expires" == attributeName || "port" == attributeName
                    ) ";" else ";,"
                    var attributeValue: String? = null
                    if (readEqualsSign()) {
                        attributeValue = readAttributeValue(terminators)
                    }
                    setAttribute(cookie, attributeName, attributeValue)
                }
                if (hasExpires) {
                    cookie.version = 0
                } else if (hasMaxAge) {
                    cookie.version = 1
                }
            }
        }

        @ExperimentalTime
        private fun setAttribute(cookie: HttpCookie, name: String, value: String?) {
            if (name == "comment" && cookie.comment == null) {
                cookie.comment = value
            } else if (name == "commenturl" && cookie.commentURL == null) {
                cookie.commentURL = value
            } else if (name == "discard") {
                cookie.discard = true
            } else if (name == "domain" && cookie.domain == null) {
                cookie.domain = value
            } else if (name == "expires") {
                hasExpires = true
                if (cookie.maxAge == -1L) {
                    val date = parseHttpDate(value)
                    if (date != null) {
                        cookie.setExpires(date)
                    } else {
                        cookie.maxAge = 0
                    }
                }
            } else if (name == "max-age" && cookie.maxAge == -1L) {
                hasMaxAge = true
                cookie.maxAge = value!!.toLong()
            } else if (name == "path" && cookie.path == null) {
                cookie.path = value
            } else if (name == "port" && cookie.portlist == null) {
                cookie.portlist = value ?: ""
            } else if (name == "secure") {
                cookie.secure = true
            } else if (name == "version" && !hasVersion) {
                cookie.version = value!!.toInt()
            }
        }

        private fun parseHttpDate(value: String?): Instant? {
            try {
                return Instant.parse(value!!)
            } catch (ignore: Exception) {
            }
            return null
        }

        /**
         * Returns the next attribute name, or null if the input has been
         * exhausted. Returns wth the cursor on the delimiter that follows.
         */
        private fun readAttributeName(returnLowerCase: Boolean): String? {
            skipWhitespace()
            val c = find(ATTRIBUTE_NAME_TERMINATORS)
            val forSubstring = if (returnLowerCase) inputLowerCase else input
            val result = if (pos < c) forSubstring.substring(pos, c) else null
            pos = c
            return result
        }

        /**
         * Returns true if an equals sign was read and consumed.
         */
        private fun readEqualsSign(): Boolean {
            skipWhitespace()
            if (pos < input.length && input[pos] == '=') {
                pos++
                return true
            }
            return false
        }

        /**
         * Reads an attribute value, by parsing either a quoted string or until
         * the next character in `terminators`. The terminator character
         * is not consumed.
         */
        private fun readAttributeValue(terminators: String): String {
            skipWhitespace()
            /*
             * Quoted string: read 'til the close quote. The spec mentions only "double quotes"
             * but RI bug 6901170 claims that 'single quotes' are also used.
             */if (pos < input.length && (input[pos] == '"' || input[pos] == '\'')) {
                val quoteCharacter = input[pos++]
                val closeQuote = input.indexOf(quoteCharacter, pos)
                require(closeQuote != -1) { "Unterminated string literal in $input" }
                val result = input.substring(pos, closeQuote)
                pos = closeQuote + 1
                return result
            }
            val c = find(terminators)
            val result = input.substring(pos, c)
            pos = c
            return result
        }

        /**
         * Returns the index of the next character in `chars`, or the end
         * of the string.
         */
        private fun find(chars: String): Int {
            for (c in pos until input.length) {
                if (chars.indexOf(input[c]) != -1) {
                    return c
                }
            }
            return input.length
        }

        private fun skipWhitespace() {
            while (pos < input.length) {
                if (WHITESPACE.indexOf(input[pos]) == -1) {
                    break
                }
                pos++
            }
        }

        companion object {
            private const val ATTRIBUTE_NAME_TERMINATORS = ",;= \t"
            private const val WHITESPACE = " \t"
        }

        init {
            inputLowerCase = input.toLowerCase()
        }
    }

    private var comment: String? = null
    private var commentURL: String? = null
    private var discard = false
    private var domain: String? = null
    private var maxAge = -1L

    /**
     * Returns the name of this cookie.
     */
    val name: String
    private var path: String? = null
    /**
     * Returns the `Port` attribute, usually containing comma-separated
     * port numbers. A null port indicates that the cookie may be sent to any
     * port. The empty string indicates that the cookie should only be sent to
     * the port of the originating request.
     */
    /**
     * Set the `Port` attribute of this cookie.
     */
    var portlist: String? = null
    private var secure = false
    /**
     * Returns the value of this cookie.
     */// FIXME: According to spec, version 0 cookie value does not allow many
    // symbols. But RI does not implement it. Follow RI temporarily.
    /**
     * Sets the opaque value of this cookie.
     */
    var value: String
    private var version = 1
    private fun isValidName(n: String): Boolean {
        // name cannot be empty or begin with '$' or equals the reserved
        // attributes (case-insensitive)
        var isValid = !(n.length == 0 || n.startsWith("$")
                || RESERVED_NAMES.contains(n.toLowerCase()))
        if (isValid) {
            for (i in 0 until n.length) {
                val nameChar = n[i]
                // name must be ASCII characters and cannot contain ';', ',' and
                // whitespace
                if (nameChar.toInt() < 0 || nameChar.toInt() >= 127 || nameChar == ';' || nameChar == ',' || nameChar.isWhitespace() && nameChar != ' ') {
                    isValid = false
                    break
                }
            }
        }
        return isValid
    }

    /**
     * Returns the `Comment` attribute.
     */
    fun getComment(): String? {
        return comment
    }

    /**
     * Returns the value of `CommentURL` attribute.
     */
    fun getCommentURL(): String? {
        return commentURL
    }

    /**
     * Returns the `Discard` attribute.
     */
    fun getDiscard(): Boolean {
        return discard
    }

    /**
     * Returns the `Domain` attribute.
     */
    fun getDomain(): String? {
        return domain
    }

    /**
     * Returns the `Max-Age` attribute, in delta-seconds.
     */
    fun getMaxAge(): Long {
        return maxAge
    }

    /**
     * Returns the `Path` attribute. This cookie is visible to all
     * subpaths.
     */
    fun getPath(): String? {
        return path
    }

    /**
     * Returns the `Secure` attribute.
     */
    fun getSecure(): Boolean {
        return secure
    }

    /**
     * Returns the version of this cookie.
     */
    fun getVersion(): Int {
        return version
    }

    /**
     * Returns true if this cookie's Max-Age is 0.
     */
    fun hasExpired(): Boolean {
        // -1 indicates the cookie will persist until browser shutdown
        // so the cookie is not expired.
        if (maxAge == -1L) {
            return false
        }
        var expired = false
        if (maxAge <= 0L) {
            expired = true
        }
        return expired
    }

    /**
     * Set the `Comment` attribute of this cookie.
     */
    fun setComment(comment: String?) {
        this.comment = comment
    }

    /**
     * Set the `CommentURL` attribute of this cookie.
     */
    fun setCommentURL(commentURL: String?) {
        this.commentURL = commentURL
    }

    /**
     * Set the `Discard` attribute of this cookie.
     */
    fun setDiscard(discard: Boolean) {
        this.discard = discard
    }

    /**
     * Set the `Domain` attribute of this cookie. HTTP clients send
     * cookies only to matching domains.
     */
    fun setDomain(pattern: String?) {
        domain = pattern?.toLowerCase()
    }

    /**
     * Sets the `Max-Age` attribute of this cookie.
     */
    fun setMaxAge(deltaSeconds: Long) {
        maxAge = deltaSeconds
    }

    @ExperimentalTime
    private fun setExpires(expires: Instant) {
        maxAge = (expires - Clock.System.now()).toLongMilliseconds() / 1000
    }

    /**
     * Set the `Path` attribute of this cookie. HTTP clients send cookies
     * to this path and its subpaths.
     */
    fun setPath(path: String?) {
        this.path = path
    }

    /**
     * Sets the `Secure` attribute of this cookie.
     */
    fun setSecure(secure: Boolean) {
        this.secure = secure
    }

    /**
     * Sets the `Version` attribute of the cookie.
     *
     * @throws IllegalArgumentException if v is neither 0 nor 1
     */
    fun setVersion(v: Int) {
        require(!(v != 0 && v != 1))
        version = v
    }

    /**
     * Returns true if `object` is a cookie with the same domain, name and
     * path. Domain and name use case-insensitive comparison; path uses a
     * case-sensitive comparison.
     */
    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) {
            return true
        }
        if (`object` is HttpCookie) {
            val that = `object`
            return (name.equals(that.name, ignoreCase = true)
                    && (if (domain != null) domain.equals(
                that.domain,
                ignoreCase = true
            ) else that.domain == null)
                    && if (path != null) path.equals(
                that.path,
                ignoreCase = true
            ) else that.path == null)
        }
        return false
    }

    /**
     * Returns the hash code of this HTTP cookie: <pre>   `name.toLowerCase(Locale.US).hashCode()
     * + (domain == null ? 0 : domain.toLowerCase(Locale.US).hashCode())
     * + (path == null ? 0 : path.hashCode())
    `</pre> *
     */
    override fun hashCode(): Int {
        return (name.toLowerCase().hashCode()
                + (if (domain == null) 0 else domain!!.toLowerCase().hashCode())
                + if (path == null) 0 else path.hashCode())
    }

    /**
     * Returns a string representing this cookie in the format used by the
     * `Cookie` header line in an HTTP request.
     */
    override fun toString(): String {
        if (version == 0) {
            return "$name=$value"
        }
        val result = StringBuilder()
            .append(name)
            .append("=")
            .append("\"")
            .append(value)
            .append("\"")
        appendAttribute(result, "Path", path)
        appendAttribute(result, "Domain", domain)
        appendAttribute(result, "Port", portlist)
        return result.toString()
    }

    private fun appendAttribute(builder: StringBuilder?, name: String, value: String?) {
        if (value != null && builder != null) {
            builder.append(";$")
            builder.append(name)
            builder.append("=\"")
            builder.append(value)
            builder.append("\"")
        }
    }

    /**
     * Creates a new cookie.
     *
     * @param name a non-empty string that contains only printable ASCII, no
     * commas or semicolons, and is not prefixed with  `$`. May not be
     * an HTTP attribute name.
     * @param value an opaque value from the HTTP server.
     * @throws IllegalArgumentException if `name` is invalid.
     */
    init {
        val ntrim = name.trim { it <= ' ' } // erase leading and trailing whitespace
        require(isValidName(ntrim))
        this.name = ntrim
        this.value = value
    }
}