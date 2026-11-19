package br.com.plantaoclima.service;

import br.com.plantaoclima.dao.RelatorioDao;
import br.com.plantaoclima.model.RelatorioDtos.CoberturaAlertaDto;
import br.com.plantaoclima.model.RelatorioDtos.PainelResumoDto;
import br.com.plantaoclima.model.RelatorioDtos.RedeIncompletaDto;
import br.com.plantaoclima.model.RelatorioDtos.RelatorioAgregadoRegiaoDto;
import br.com.plantaoclima.model.RelatorioDtos.TaxaNaoAtendeuDto;

import java.util.List;

public class RelatorioService {

    private final RelatorioDao relatorioDao;

    public RelatorioService(RelatorioDao relatorioDao) {
        this.relatorioDao = relatorioDao;
    }

    public PainelResumoDto obterResumoPainel() {
        return relatorioDao.obterResumoPainel();
    }

    public List<CoberturaAlertaDto> listarCoberturaAte4Horas() {
        return relatorioDao.listarCoberturaAte4Horas();
    }

    public List<RedeIncompletaDto> listarIdososComRedeIncompleta() {
        return relatorioDao.listarIdososComRedeIncompleta();
    }

    public List<TaxaNaoAtendeuDto> listarTaxaNaoAtendeuPorVoluntario() {
        return relatorioDao.listarTaxaNaoAtendeuPorVoluntario();
    }

    public List<RelatorioAgregadoRegiaoDto> listarRelatorioAgregadoPorRegiao() {
        return relatorioDao.listarRelatorioAgregadoPorRegiao();
    }
}
