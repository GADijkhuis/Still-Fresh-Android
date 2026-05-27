import org.mindrot.jbcrypt.BCrypt

object CryptoHelper {

    fun generateSalt(): String {
        return BCrypt.gensalt(12)
    }

    fun hashPassword(password: String, salt: String): String {
        return BCrypt.hashpw(password, salt)
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        return BCrypt.checkpw(password, storedHash)
    }
}