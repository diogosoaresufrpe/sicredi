package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface PagadorRepository extends JpaRepository<Pagador, Long>  {

    List<Pagador> findByCpfCnpjAndAgenciaAndContaAndTipoConta(String cpfCnpj, String agencia, String conta, TipoContaEnum tipoConta);

}
