package com.example.demo.config;

import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import com.example.demo.models.Orcamento;
import com.example.demo.models.Tarefa;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.OrcamentoRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.repositories.LancamentoCustoRepository;
import com.example.demo.models.LancamentoCusto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("obraAuthorization")
public class ObraAuthorization {
    private final ObraRepository obraRepository;
    private final EtapaRepository etapaRepository;
    private final TarefaRepository tarefaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LancamentoCustoRepository lancamentoRepository;

    public ObraAuthorization(
            ObraRepository obraRepository,
            EtapaRepository etapaRepository,
            TarefaRepository tarefaRepository,
            OrcamentoRepository orcamentoRepository,
            UsuarioRepository usuarioRepository,
            LancamentoCustoRepository lancamentoRepository
    ) {
        this.obraRepository = obraRepository;
        this.etapaRepository = etapaRepository;
        this.tarefaRepository = tarefaRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    public boolean podeAcessarObra(Long obraId, Authentication authentication) {
        if (administrador(authentication)) {
            return true;
        }
        if (authentication == null || obraId == null) {
            return false;
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        Obra obra = obraRepository.findById(obraId).orElse(null);
        if (usuario == null || obra == null || obra.getDataDelecao() != null) {
            return false;
        }
        return obraRepository.usuarioClienteDaObra(obraId, usuario.getId())
                || obraRepository.usuarioVinculadoAObra(obraId, usuario.getId());
    }

    public boolean podeAcessarEtapa(Long etapaId, Authentication authentication) {
        Etapa etapa = etapaRepository.findById(etapaId).orElse(null);
        return etapa != null && podeAcessarObra(etapa.getObra().getId(), authentication);
    }

    public boolean podeAcessarTarefa(Long tarefaId, Authentication authentication) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId).orElse(null);
        return tarefa != null && podeAcessarEtapa(tarefa.getEtapa().getId(), authentication);
    }

    public boolean podeAcessarVersao(Long versaoId, Authentication authentication) {
        Orcamento versao = orcamentoRepository.findById(versaoId).orElse(null);
        return versao != null && podeAcessarObra(versao.getObra().getId(), authentication);
    }

    public boolean podeAcessarLancamento(Long lancamentoId, Authentication authentication) {
        LancamentoCusto lancamento = lancamentoRepository.findById(lancamentoId).orElse(null);
        return lancamento != null && podeAcessarEtapa(lancamento.getEtapa().getId(), authentication);
    }

    private boolean administrador(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
