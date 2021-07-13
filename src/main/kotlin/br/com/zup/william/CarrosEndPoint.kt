package br.com.zup.william

import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class CarrosEndpoint(val carroRepository: CarroRepository) : CarrosServiceGrpc.CarrosServiceImplBase() {
    override fun send(request: CarrosRequest, responseObserver: StreamObserver<CarrosResponse>) {

        if (carroRepository.existsByPlaca(request.placa)) {
            responseObserver.onError(
                Status.ALREADY_EXISTS
                    .withDescription("Já existe um veículo com essa placa.")
                    .asRuntimeException()
            )
            return
        }

        val novoCarro = Carro(placa = request.placa, modelo = request.modelo)
        try {
            carroRepository.save(novoCarro)
        } catch (e: ConstraintViolationException) {
            responseObserver.onError(
               Status.INVALID_ARGUMENT
                    .withDescription("dados de entrada inválidos.")
                    .asRuntimeException()
            )
            return
        }

        responseObserver.onNext(CarrosResponse.newBuilder().setId(novoCarro.id!!).build())
        responseObserver.onCompleted()
    }
}
