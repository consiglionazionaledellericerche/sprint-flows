package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Faq;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.FaqRepository;
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
 * Test class for the FaqResource REST controller.
 *
 * @see FaqResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class FaqResourceIntTest {
    private static final String DEFAULT_DOMANDA = "AAAAA";
    private static final String UPDATED_DOMANDA = "BBBBB";
    private static final String DEFAULT_RISPOSTA = "AAAAA";
    private static final String UPDATED_RISPOSTA = "BBBBB";

    private static final Boolean DEFAULT_IS_READABLE = false;
    private static final Boolean UPDATED_IS_READABLE = true;

    @Inject
    private FaqRepository faqRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restFaqMockMvc;

    private Faq faq;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        FaqResource faqResource = new FaqResource();
        ReflectionTestUtils.setField(faqResource, "faqRepository", faqRepository);
        this.restFaqMockMvc = MockMvcBuilders.standaloneSetup(faqResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Faq createEntity(EntityManager em) {
        Faq faq = new Faq();
        faq = new Faq()
                .domanda(DEFAULT_DOMANDA)
                .risposta(DEFAULT_RISPOSTA)
                .isReadable(DEFAULT_IS_READABLE);
        return faq;
    }

    @Before
    public void initTest() {
        faq = createEntity(em);
    }

    @Test
    @Transactional
    public void createFaq() throws Exception {
        int databaseSizeBeforeCreate = faqRepository.findAll().size();

        // Create the Faq

        restFaqMockMvc.perform(post("/api/faqs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(faq)))
                .andExpect(status().isCreated());

        // Validate the Faq in the database
        List<Faq> faqs = faqRepository.findAll();
        assertThat(faqs).hasSize(databaseSizeBeforeCreate + 1);
        Faq testFaq = faqs.get(faqs.size() - 1);
        assertThat(testFaq.getDomanda()).isEqualTo(DEFAULT_DOMANDA);
        assertThat(testFaq.getRisposta()).isEqualTo(DEFAULT_RISPOSTA);
        assertThat(testFaq.isIsReadable()).isEqualTo(DEFAULT_IS_READABLE);
    }

    @Test
    @Transactional
    public void checkDomandaIsRequired() throws Exception {
        int databaseSizeBeforeTest = faqRepository.findAll().size();
        // set the field null
        faq.setDomanda(null);

        // Create the Faq, which fails.

        restFaqMockMvc.perform(post("/api/faqs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(faq)))
                .andExpect(status().isBadRequest());

        List<Faq> faqs = faqRepository.findAll();
        assertThat(faqs).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkRispostaIsRequired() throws Exception {
        int databaseSizeBeforeTest = faqRepository.findAll().size();
        // set the field null
        faq.setRisposta(null);

        // Create the Faq, which fails.

        restFaqMockMvc.perform(post("/api/faqs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(faq)))
                .andExpect(status().isBadRequest());

        List<Faq> faqs = faqRepository.findAll();
        assertThat(faqs).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkIsReadableIsRequired() throws Exception {
        int databaseSizeBeforeTest = faqRepository.findAll().size();
        // set the field null
        faq.setIsReadable(null);

        // Create the Faq, which fails.

        restFaqMockMvc.perform(post("/api/faqs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(faq)))
                .andExpect(status().isBadRequest());

        List<Faq> faqs = faqRepository.findAll();
        assertThat(faqs).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllFaqs() throws Exception {
        // Initialize the database
        faqRepository.saveAndFlush(faq);

        // Get all the faqs
        restFaqMockMvc.perform(get("/api/faqs?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(faq.getId().intValue())))
                .andExpect(jsonPath("$.[*].domanda").value(hasItem(DEFAULT_DOMANDA.toString())))
                .andExpect(jsonPath("$.[*].risposta").value(hasItem(DEFAULT_RISPOSTA.toString())))
                .andExpect(jsonPath("$.[*].isReadable").value(hasItem(DEFAULT_IS_READABLE.booleanValue())));
    }

    @Test
    @Transactional
    public void getFaq() throws Exception {
        // Initialize the database
        faqRepository.saveAndFlush(faq);

        // Get the faq
        restFaqMockMvc.perform(get("/api/faqs/{id}", faq.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(faq.getId().intValue()))
            .andExpect(jsonPath("$.domanda").value(DEFAULT_DOMANDA.toString()))
            .andExpect(jsonPath("$.risposta").value(DEFAULT_RISPOSTA.toString()))
            .andExpect(jsonPath("$.isReadable").value(DEFAULT_IS_READABLE.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingFaq() throws Exception {
        // Get the faq
        restFaqMockMvc.perform(get("/api/faqs/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateFaq() throws Exception {
        // Initialize the database
        faqRepository.saveAndFlush(faq);
        int databaseSizeBeforeUpdate = faqRepository.findAll().size();

        // Update the faq
        Faq updatedFaq = faqRepository.findById(faq.getId()).get();
        updatedFaq
                .domanda(UPDATED_DOMANDA)
                .risposta(UPDATED_RISPOSTA)
                .isReadable(UPDATED_IS_READABLE);

        restFaqMockMvc.perform(put("/api/faqs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedFaq)))
                .andExpect(status().isOk());

        // Validate the Faq in the database
        List<Faq> faqs = faqRepository.findAll();
        assertThat(faqs).hasSize(databaseSizeBeforeUpdate);
        Faq testFaq = faqs.get(faqs.size() - 1);
        assertThat(testFaq.getDomanda()).isEqualTo(UPDATED_DOMANDA);
        assertThat(testFaq.getRisposta()).isEqualTo(UPDATED_RISPOSTA);
        assertThat(testFaq.isIsReadable()).isEqualTo(UPDATED_IS_READABLE);
    }

    @Test
    @Transactional
    public void deleteFaq() throws Exception {
        // Initialize the database
        faqRepository.saveAndFlush(faq);
        int databaseSizeBeforeDelete = faqRepository.findAll().size();

        // Get the faq
        restFaqMockMvc.perform(delete("/api/faqs/{id}", faq.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Faq> faqs = faqRepository.findAll();
        assertThat(faqs).hasSize(databaseSizeBeforeDelete - 1);
    }
}
