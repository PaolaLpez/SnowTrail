package mx.utng.snowtrail

import mx.utng.snowtrail.model.NeveriaResumen
import mx.utng.snowtrail.model.PedidoResumen
import mx.utng.snowtrail.model.ProductoResumen
import mx.utng.snowtrail.service.WearStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StateHolderTest {

    @Test
    fun testActiveOrderStateHolder() {
        // Assert initial state is null
        assertNull(WearStateHolder.activeOrder.value)

        // Create sample order
        val testOrder = PedidoResumen(
            id = "ord_test_001",
            neveriaId = "nev_test",
            neveriaNombre = "Nevería de Prueba",
            estado = "ACEPTADO",
            tiempoEstimadoMinutos = 15,
            fechaHoraMillis = 1700000000000L,
            total = 120.0,
            productos = listOf(
                ProductoResumen("Nieve de Mango", 2, 40.0),
                ProductoResumen("Helado Menta", 1, 40.0)
            )
        )

        // Update state holder
        WearStateHolder.updateActiveOrder(testOrder)

        // Verify state is updated
        assertEquals(testOrder, WearStateHolder.activeOrder.value)
        assertEquals("ACEPTADO", WearStateHolder.activeOrder.value?.estado)
        assertEquals(2, WearStateHolder.activeOrder.value?.productos?.size)
    }

    @Test
    fun testNearbyShopsStateHolder() {
        // Assert initial state is empty list
        assertEquals(0, WearStateHolder.nearbyShops.value.size)

        val shops = listOf(
            NeveriaResumen("1", "Shop A", 150.0, true, true),
            NeveriaResumen("2", "Shop B", 450.0, false, false)
        )

        // Update state holder
        WearStateHolder.updateNearbyShops(shops)

        // Verify state is updated
        assertEquals(2, WearStateHolder.nearbyShops.value.size)
        assertEquals("Shop A", WearStateHolder.nearbyShops.value[0].nombre)
        assertEquals(true, WearStateHolder.nearbyShops.value[0].esFavorita)
    }
}
