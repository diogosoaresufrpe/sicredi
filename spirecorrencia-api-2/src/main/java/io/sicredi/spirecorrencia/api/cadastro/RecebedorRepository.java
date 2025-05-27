package io.sicredi.spirecorrencia.api.cadastro;

import io.sicredi.spirecorrencia.api.repositorio.Recebedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface RecebedorRepository extends JpaRepository<Recebedor, Long> {

    @Query("""
            SELECT r
            FROM Recebedor r
            WHERE r.cpfCnpj = :#{#recebedorRequestDto.cpfCnpj}
            AND r.nome = :#{#recebedorRequestDto.nome}
            AND r.conta = :#{#recebedorRequestDto.conta}
            AND r.instituicao = :#{#recebedorRequestDto.instituicao}
            AND ((:#{#recebedorRequestDto.agencia} IS NULL AND r.agencia IS NULL) OR r.agencia = :#{#recebedorRequestDto.agencia})
            AND ((:#{#recebedorRequestDto.chave} IS NULL AND r.chave IS NULL) OR r.chave = :#{#recebedorRequestDto.chave})
            AND ((:#{#recebedorRequestDto.tipoChave} IS NULL AND r.tipoChave IS NULL) OR r.tipoChave = :#{#recebedorRequestDto.tipoChave})
            AND r.tipoConta = :#{#recebedorRequestDto.tipoConta}
            AND r.tipoPessoa = :#{#recebedorRequestDto.tipoPessoa}
            """)
    Optional<Recebedor> buscarRecebedor(RecorrenteRecebedorDTO recebedorRequestDto);

}
