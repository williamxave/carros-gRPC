package br.com.zup.william

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
    val carroRepository: CarroRepository,
    val grpcClient: CarrosServiceGrpc.CarrosServiceBlockingStub
) {
    /**
     * 1. happy path - ok
     * 2. quando já existe carro com a placa - ok
     * 3. quando os dados de entrada são inválidos - ok
     */

    @BeforeEach
    fun antes() {
        carroRepository.deleteAll()
    }

    @Test
    fun `deve cadastrar um novo carro`() {

        //cenario

        //acao
        val response = grpcClient.send(
            CarrosRequest.newBuilder()
                .setModelo("Gol bola")
                .setPlaca("00000-000")
                .build()
        )
        //validacao quando temos varios asserts e eles compartilham o argumento, é elegante usar o with
        with(response) {
            assertNotNull(id)
            assertTrue(carroRepository.existsById(id)) // Efeito colateral
        }
    }

    @Test
    fun `nao deve cadastrar um carro novo se a placa ja existir`() {
        //cenario
        val novoCarro = Carro("Premio 77", "11111-111")
        carroRepository.save(novoCarro)

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.send(
                CarrosRequest.newBuilder()
                    .setModelo("Chevette 77")
                    .setPlaca("11111-111")
                    .build()
            )
        }

        //validacao
        with(error){
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Já existe um veículo com essa placa.", status.description)
        }
    }

    @Test
    fun `nao deve cadastrar novo carro com dados invalidos`(){
        //cenario

        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.send(CarrosRequest.newBuilder().build())
        }

        //validacao
        with(error){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos.", status.description)
        }
    }



    @Factory
    class Clients {
        @Singleton
        fun blockingStup(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosServiceGrpc.CarrosServiceBlockingStub {
            return CarrosServiceGrpc.newBlockingStub(channel)
        }
    }

}