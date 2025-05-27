package io.sicredi.spirecorrencia.api.repositorio;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "RECORRENCIA_RECEBEDOR", schema = "SPI_OWNER")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Recebedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OID_RECORRENCIA_RECEBEDOR", nullable = false)
    private Long oidRecebedor;

    @Column(name = "NUM_CPF_CNPJ", nullable = false, length = 14)
    private String cpfCnpj;

    @Column(name = "NOME", nullable = false, length = 140)
    private String nome;

    @Column(name = "NUM_AGENCIA", length = 4)
    private String agencia;

    @Column(name = "NUM_CONTA", nullable = false, length = 20)
    private String conta;

    @Column(name = "NUM_INSTITUICAO", nullable = false, length = 8)
    private String instituicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_CONTA", nullable = false, length = 25)
    private TipoContaEnum tipoConta;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_PESSOA", nullable = false, length = 2)
    private TipoPessoaEnum tipoPessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_CHAVE", length = 8)
    private TipoChaveEnum tipoChave;

    @Column(name = "CHAVE", length = 77)
    private String chave;

    @Column(name = "DAT_CRIACAO_REGISTRO", updatable = false)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column (name = "DAT_ALTERACAO_REGISTRO")
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;

}
