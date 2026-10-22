package com.cashi.app.data.mapper

import com.cashi.app.data.remote.dto.PaymentDto
import com.cashi.app.data.remote.dto.PaymentRequestDto
import com.cashi.app.data.remote.dto.UserDto
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.model.User

fun PaymentDto.toDomain() = Payment(
    id = id,
    amount = amount,
    currency = currency,
    recipientEmail = recipientEmail,
    description = description?.takeIf { it.isNotBlank() },
    createdAt = createdAt,
)

fun NewPayment.toRequest() = PaymentRequestDto(
    amount = amount,
    currency = currency.code,
    recipientEmail = recipientEmail,
    description = description,
)

fun UserDto.toDomain() = User(id = id, email = email, fullName = fullName)
