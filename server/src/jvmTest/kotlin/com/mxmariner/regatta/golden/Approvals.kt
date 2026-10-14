package com.mxmariner.regatta.golden

import java.io.File

/**
 * A minimal approval helper. Each named output is compared, byte for byte, to
 * `src/jvmTest/resources/golden/approved/<name>.json` (relative to the server project, which is
 * the test task's working directory).
 *
 * - On a mismatch, or when no approved file exists, the output is written to
 *   `<name>.received.json` next to it (git-ignored) and the name is recorded in [mismatches].
 * - With `-Dgolden.approve=true` the approved file is overwritten instead and any stale
 *   received file is deleted.
 *
 * Call [assertAllApproved] at the end of a test to fail once with every mismatch listed.
 */
class Approvals(private val dir: File = File("src/jvmTest/resources/golden/approved")) {
    private val approve = System.getProperty("golden.approve") == "true"
    private val mismatches = mutableListOf<String>()
    private val approvedNames = mutableListOf<String>()

    init {
        require(dir.isDirectory || dir.mkdirs()) { "cannot create ${dir.absolutePath}" }
    }

    fun check(name: String, content: String) {
        val approved = File(dir, "$name.json")
        val received = File(dir, "$name.received.json")
        if (approve) {
            approved.writeText(content)
            received.delete()
            approvedNames.add(name)
            return
        }
        if (approved.isFile && approved.readText() == content) {
            received.delete()
            return
        }
        received.writeText(content)
        mismatches.add(if (approved.isFile) "$name (differs)" else "$name (no approved file)")
    }

    fun assertAllApproved() {
        if (approve) {
            println("approved ${approvedNames.size} golden file(s) in ${dir.absolutePath}")
            return
        }
        if (mismatches.isNotEmpty()) {
            throw AssertionError(
                "${mismatches.size} golden output(s) differ from ${dir.absolutePath}. " +
                        "Compare the .received.json files, or re-approve with -Dgolden.approve=true:\n  " +
                        mismatches.joinToString("\n  ")
            )
        }
    }
}
