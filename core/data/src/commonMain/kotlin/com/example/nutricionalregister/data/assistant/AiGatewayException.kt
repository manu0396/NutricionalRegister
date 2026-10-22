package com.example.nutricionalregister.data.assistant

/**
 * The single failure the AI gateway can raise.
 *
 * Every way the conversation can go wrong - a failed status, a network that is
 * not there, a reply the app cannot read - arrives as this one type, so the
 * presentation layer can show one message instead of a family of technical
 * errors (FR-025, FR-027).
 *
 * @property message the reason in words the user can read.
 */
public class AiGatewayException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
