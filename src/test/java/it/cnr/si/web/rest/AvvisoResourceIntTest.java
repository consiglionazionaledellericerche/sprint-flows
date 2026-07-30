package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Avviso;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.AvvisoRepository;
import it.cnr.si.service.AvvisoService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the AvvisoResource REST controller.
 *
 * @see AvvisoResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class AvvisoResourceIntTest {
    private static final String DEFAULT_CONTENUTO = "AAAAA";
    private static final String UPDATED_CONTENUTO = "BBBBB";

    private static final Boolean DEFAULT_ATTIVO = false;
    private static final Boolean UPDATED_ATTIVO = true;

    @Inject
    private AvvisoRepository avvisoRepository;

    @Inject
    private AvvisoService avvisoService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restAvvisoMockMvc;

    private Avviso avviso;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        AvvisoResource avvisoResource = new AvvisoResource();
        ReflectionTestUtils.setField(avvisoResource, "avvisoService", avvisoService);
        this.restAvvisoMockMvc = MockMvcBuilders.standaloneSetup(avvisoResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Avviso createEntity(EntityManager em) {
        Avviso avviso = new Avviso();
        avviso = new Avviso()
                .contenuto(DEFAULT_CONTENUTO)
                .attivo(DEFAULT_ATTIVO);
        return avviso;
    }

    @Before
    public void initTest() {
        avviso = createEntity(em);
    }

    @Test
    @Transactional
    public void createAvviso() throws Exception {
        int databaseSizeBeforeCreate = avvisoRepository.findAll().size();

        // Create the Avviso

        restAvvisoMockMvc.perform(post("/api/avvisos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(avviso)))
                .andExpect(status().isCreated());

        // Validate the Avviso in the database
        List<Avviso> avvisos = avvisoRepository.findAll();
        assertThat(avvisos).hasSize(databaseSizeBeforeCreate + 1);
        Avviso testAvviso = avvisos.get(avvisos.size() - 1);
        assertThat(testAvviso.getContenuto()).isEqualTo(DEFAULT_CONTENUTO);
        assertThat(testAvviso.isAttivo()).isEqualTo(DEFAULT_ATTIVO);
    }

    @Test
    @Transactional
    public void checkContenutoIsRequired() throws Exception {
        int databaseSizeBeforeTest = avvisoRepository.findAll().size();
        // set the field null
        avviso.setContenuto(null);

        // Create the Avviso, which fails.

        restAvvisoMockMvc.perform(post("/api/avvisos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(avviso)))
                .andExpect(status().isBadRequest());

        List<Avviso> avvisos = avvisoRepository.findAll();
        assertThat(avvisos).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllAvvisos() throws Exception {
        // Initialize the database
        avvisoRepository.saveAndFlush(avviso);

        // Get all the avvisos
        restAvvisoMockMvc.perform(get("/api/avvisos?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(avviso.getId().intValue())))
                .andExpect(jsonPath("$.[*].contenuto").value(hasItem(DEFAULT_CONTENUTO.toString())))
                .andExpect(jsonPath("$.[*].attivo").value(hasItem(DEFAULT_ATTIVO.booleanValue())));
    }

    @Test
    @Transactional
    public void getAvviso() throws Exception {
        // Initialize the database
        avvisoRepository.saveAndFlush(avviso);

        // Get the avviso
        restAvvisoMockMvc.perform(get("/api/avvisos/{id}", avviso.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(avviso.getId().intValue()))
                .andExpect(jsonPath("$.contenuto").value(DEFAULT_CONTENUTO.toString()))
                .andExpect(jsonPath("$.attivo").value(DEFAULT_ATTIVO.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingAvviso() throws Exception {
        // Get the avviso
        restAvvisoMockMvc.perform(get("/api/avvisos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateAvviso() throws Exception {
        // Initialize the database
        avvisoService.save(avviso);

        int databaseSizeBeforeUpdate = avvisoRepository.findAll().size();

        // Update the avviso
        Avviso updatedAvviso = avvisoRepository.findById(avviso.getId()).get();
        updatedAvviso
                .contenuto(UPDATED_CONTENUTO)
                .attivo(UPDATED_ATTIVO);

        restAvvisoMockMvc.perform(put("/api/avvisos")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedAvviso)))
                .andExpect(status().isOk());

        // Validate the Avviso in the database
        List<Avviso> avvisos = avvisoRepository.findAll();
        assertThat(avvisos).hasSize(databaseSizeBeforeUpdate);
        Avviso testAvviso = avvisos.get(avvisos.size() - 1);
        assertThat(testAvviso.getContenuto()).isEqualTo(UPDATED_CONTENUTO);
        assertThat(testAvviso.isAttivo()).isEqualTo(UPDATED_ATTIVO);
    }

    @Test
    @Transactional
    public void deleteAvviso() throws Exception {
        // Initialize the database
        avvisoService.save(avviso);

        int databaseSizeBeforeDelete = avvisoRepository.findAll().size();

        // Get the avviso
        restAvvisoMockMvc.perform(delete("/api/avvisos/{id}", avviso.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Avviso> avvisos = avvisoRepository.findAll();
        assertThat(avvisos).hasSize(databaseSizeBeforeDelete - 1);
    }
}
