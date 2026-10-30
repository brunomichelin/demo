package com.example.demo.controllers;

import com.example.demo.models.Aluno;
import com.example.demo.repositories.AlunoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

    @Autowired
    private AlunoRepositorio alunoRepositorio;

    @GetMapping
    public Page<Aluno> listar(Pageable pageable) {
        return alunoRepositorio.findAllOrderByMatricula(pageable);
    }

    @PostMapping
    public void incluir(@RequestBody Aluno aluno) {
        alunoRepositorio.save(aluno);
    }

    @PutMapping
    public void alterar(@RequestBody Aluno aluno) {
        alunoRepositorio.save(aluno);
    }

    @DeleteMapping("/{matricula}")
    public void deletar(@PathVariable Long matricula) {
        alunoRepositorio.deleteById(matricula);
    }

    @GetMapping("/{matricula}")
    public Optional<Aluno> ler(@PathVariable Long matricula) {
        return alunoRepositorio.findById(matricula);
    }

}
