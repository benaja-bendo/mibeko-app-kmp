package com.mibeko.mibeko.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportEntitlementTest {

    @Test
    fun `403 est refuse pour entitlement absent`() {
        assertTrue(isExportEntitlementDenied(403))
    }

    @Test
    fun `404 n'est pas un refus d'entitlement`() {
        assertFalse(isExportEntitlementDenied(404))
    }

    @Test
    fun `absence de statut n'est pas un refus d'entitlement`() {
        assertFalse(isExportEntitlementDenied(null))
    }
}
