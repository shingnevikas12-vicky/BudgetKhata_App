package com.budgetkhata.data.local

import androidx.room.TypeConverter
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.entity.ExpenseCategory
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.data.local.entity.PaymentSource

class Converters {

    @TypeConverter
    fun eventTypeToString(value: EventType): String = value.name

    @TypeConverter
    fun stringToEventType(value: String): EventType = EventType.fromName(value)

    @TypeConverter
    fun paymentMethodToString(value: PaymentMethod): String = value.name

    @TypeConverter
    fun stringToPaymentMethod(value: String): PaymentMethod = PaymentMethod.fromName(value)

    @TypeConverter
    fun paymentSourceToString(value: PaymentSource): String = value.name

    @TypeConverter
    fun stringToPaymentSource(value: String): PaymentSource =
        PaymentSource.entries.firstOrNull { it.name == value } ?: PaymentSource.MANUAL

    @TypeConverter
    fun expenseCategoryToString(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun stringToExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.fromName(value)
}
