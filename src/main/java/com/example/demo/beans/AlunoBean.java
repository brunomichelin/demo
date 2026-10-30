package com.example.demo.beans;

import com.example.demo.models.Aluno;
import com.example.demo.repositories.AlunoRepositorio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.apache.logging.log4j.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component("alunoBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AlunoBean {

    private Logger LOGGER = LogManager.getLogger(AlunoBean.class);

    private static final DateTimeFormatter DATA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    @Getter
    @Setter
    private List<Aluno> alunos = new ArrayList<>();

    @Getter
    @Setter
    private Aluno aluno;

    @Autowired
    private AlunoRepositorio alunoRepositorio;

    @PostConstruct
    public void init() {
        listarTodos();
    }

    public void listarTodos() {
        this.alunos = alunoRepositorio.findAllOrderByMatricula(org.springframework.data.domain.Pageable.unpaged()).toList();
    }

    public int getTamanhoDaLista() {
        return alunos.size();
    }

    public String formatarData(Instant data) {
        return data == null ? "" : DATA_FORMATTER.format(data);
    }

    public void onRowSelected(SelectEvent<Aluno> event) {
        LOGGER.info("Aluno selecionado: {}", event.getObject());
        this.aluno = event.getObject();
    }

}
