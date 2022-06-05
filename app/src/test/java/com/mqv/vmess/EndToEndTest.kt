package com.mqv.vmess

import org.junit.Assert
import org.junit.Test
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import org.signal.libsignal.protocol.util.KeyHelper

class EndToEndTest {

    @Test
    fun encrypt_and_decrypt_message() {
        val ALICE = createPerson("alice", 1, ALICE_SIGNED_PRE_KEY_ID)
        val BOB = createPerson("bob", 2, BOB_SIGNED_PRE_KEY_ID)

        val aliceInMemoryStore =
            InMemorySignalProtocolStore(ALICE.identityKeyPair, ALICE.registrationId)
        val aliceSessionBuilder = SessionBuilder(aliceInMemoryStore, BOB.address)

        aliceSessionBuilder.process(BOB.getPreKeyBundle())

        val aliceSessionCipher = SessionCipher(aliceInMemoryStore, BOB.address)
        val message = "Hello BOB, i'm alice here"
        val encryptedMessage = aliceSessionCipher.encrypt(message.toByteArray())

        println("Encrypted message was sent from Alice to Bob: ${encryptedMessage.serialize()}")

        val bobInMemoryStore =
            InMemorySignalProtocolStore(BOB.identityKeyPair, BOB.registrationId).apply {
                storePreKey(BOB.preKeyRecord.id, BOB.preKeyRecord)
                storeSignedPreKey(BOB.signedPreKeyRecord.id, BOB.signedPreKeyRecord)
            }
        val bobSessionBuilder = SessionBuilder(bobInMemoryStore, ALICE.address)

        bobSessionBuilder.process(ALICE.getPreKeyBundle())

        val bobSessionCipher = SessionCipher(bobInMemoryStore, ALICE.address)
        val decryptedMessage =
            bobSessionCipher.decrypt(PreKeySignalMessage(encryptedMessage.serialize()))
        val readableMessage = String(decryptedMessage)

        println("Decrypted message received by Bob: $readableMessage")

        Assert.assertEquals(message, readableMessage)
    }

    companion object {
        private const val ALICE_SIGNED_PRE_KEY_ID = 1
        private const val BOB_SIGNED_PRE_KEY_ID = 2

        private fun createPerson(id: String, deviceId: Int, signedPreKeyId: Int): Person {
            val registrationId = generateRegistrationKey()
            val preKeyRecord = generatePreKeyRecord()
            val identityKeyPair = generateIdentityKeyPair()
            val signedPreKey = generateSignedPreKey(identityKeyPair, signedPreKeyId)

            return Person(
                registrationId,
                preKeyRecord,
                signedPreKey,
                identityKeyPair,
                SignalProtocolAddress(id, deviceId)
            )
        }

        private fun generateRegistrationKey() =
            KeyHelper.generateRegistrationId(false)

        private fun generateKeyPair(): ECKeyPair =
            Curve.generateKeyPair()

        private fun generatePreKeyRecord(): PreKeyRecord {
            return PreKeyRecord(1, generateKeyPair())
        }

        private fun generateIdentityKeyPair(): IdentityKeyPair {
            val pair = generateKeyPair()
            val identityKey = IdentityKey(pair.publicKey)

            return IdentityKeyPair(identityKey, pair.privateKey)
        }

        private fun generateSignedPreKey(
            localIdentity: IdentityKeyPair,
            signedPreKeyId: Int
        ): SignedPreKeyRecord {
            val keyPair = Curve.generateKeyPair()
            val signature = Curve.calculateSignature(
                localIdentity.privateKey,
                keyPair.publicKey.serialize()
            )
            return SignedPreKeyRecord(
                signedPreKeyId,
                System.currentTimeMillis(),
                keyPair,
                signature
            )
        }
    }
}

data class Person(
    val registrationId: Int,
    val preKeyRecord: PreKeyRecord,
    val signedPreKeyRecord: SignedPreKeyRecord,
    val identityKeyPair: IdentityKeyPair,
    val address: SignalProtocolAddress,
) {
    fun getPreKeyBundle(): PreKeyBundle {
        return PreKeyBundle(
            registrationId,
            address.deviceId,
            preKeyRecord.id,
            preKeyRecord.keyPair.publicKey,
            signedPreKeyRecord.id,
            signedPreKeyRecord.keyPair.publicKey,
            signedPreKeyRecord.signature,
            identityKeyPair.publicKey
        )
    }
}