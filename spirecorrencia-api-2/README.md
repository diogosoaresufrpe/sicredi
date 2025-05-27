---
title: spirecorrencia-api
linktitle: <nome-componente>
geekdocCollapseSection: true
---

### Objetivo

``` text
Componente responsável pela orquestração das recorrências do PIX.
```

### Responsáveis
``` text
Equipe / resnponsáveis por manter e evoluir este componente
```

- Equipe X

### Build
``` text
Como fazer o build (opcional, caso fuja do padrão) 
```

### Referências

- *DevConsole:* https://devconsole.sicredi.in/catalog/application/spirecorrencia/spirecorrencia-api
- ... 

### Stack
```
Componentes necessários (Quais tecnologias e porquê elas são usadas)
```

## APIs

### Swagger
- DEV: https://spirecorrencia-api.dev.sicredi.cloud/swagger-ui/index.html
- UAT: https://spirecorrencia-api.uat.sicredi.cloud/swagger-ui/index.html

### **POST** /componente/recurso
  ``` text
  Objetivo da API
  ```
#### Diagrama de fluxo (principais passos)
  ![This is an alt text.](./images/template-image-1.png "This is a sample image.")

#### Detalhamento do Diagrama
  |        | Description |
  | ----------- | ----------- |
  | Validar request                    | Validar as informações da requisição |
  | Retornar ID                      | Response com as informações da operação gerada |

## Mensageria

### **Tópico:** topic-name
  ``` text
  Objetivo da integração com o Kafka
  ```

#### Diagrama de fluxo
  ![This is an alt text.](./images/template-image-2.png "This is a sample image.")

#### Detalhamento do Diagrama (principais passos)
  |        | Description |
  | ----------- | ----------- |
  | Enviar evento de notificação    | Evento para notificar o cliente e outras integrações externas |
  | Enviar Response                      | Response com as informações da operação gerada |

## Documentação Complementar

- Links externos

## Decision Logs
| Data          | Autor             | Motivo        |
| :---          |    :----          |          :--- |
| dd/MM/yyyy    | @email               | Descrição do Motivo  |
| dd/MM/yyyy    | @email               | Descrição do Motivo      |