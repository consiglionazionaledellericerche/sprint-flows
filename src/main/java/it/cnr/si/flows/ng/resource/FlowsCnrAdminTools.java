package it.cnr.si.flows.ng.resource;

import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_DESCRIZIONE;
import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_STATO;
import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_TITOLO;
import static it.cnr.si.flows.ng.utils.Utils.DESCRIZIONE;
import static it.cnr.si.flows.ng.utils.Utils.INITIATOR;
import static it.cnr.si.flows.ng.utils.Utils.STATO;
import static it.cnr.si.flows.ng.utils.Utils.TITOLO;
import static it.cnr.si.flows.ng.utils.Utils.ellipsis;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricIdentityLinkResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.Gson;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsMailService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageSender;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

@Controller
@RequestMapping("api/admin")
@Secured(AuthoritiesConstants.ADMIN)
@Profile("cnr")
public class FlowsCnrAdminTools {
    
    private final Logger log = LoggerFactory.getLogger(FlowsCnrAdminTools.class);

    @Inject
    private HistoryService historyService;
    @Inject
    private AceService aceService;
    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private ExternalMessageSender extenalMessageSender;
    @Inject
    private ProcessEngine processEngine;
    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private FlowsTaskService flowsTaskService;
    @Inject 
    private FlowsMailService flowsMailService;

    @RequestMapping(value = "/resendExternalMessages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> resendExternalMessages() {

        log.info("Resending External Messages (manual trigger)");
        extenalMessageSender.sendMessages();
        extenalMessageSender.sendErrorMessages();
        return ResponseEntity.ok().build();
    }
    
    @RequestMapping(value = "/resendScheduledEmails", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> resendScheduledEmails() {

        log.info("Resending Scheduled Emails (manual trigger)");
        flowsMailService.sendScheduledNotifications();
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "firma-errata-missioni", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Map<String, Set<String>>>> getErroriFirmaMissioni() {
        
        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey("missioni")
            .finished()
            .list();
        
        int total = processInstances.size();
        log.info(""+total);
        
//        List<String> pids = Arrays.asList(    "19890897",     "22932462",     "20231944",     "20233677",     "20234230",     "20234472",     "20234742",     "20235667",     "24428711",     "20023707",     "22761285",     "23744181",     "21103278",     "21106339",     "21445687",     "24608655",     "25025598",     "14241522",     "14241522",     "18161568",     "18161568",     "21537886",     "21625024",     "22172936",     "15512417",     "15514925",     "15592729",     "16742543",     "16806665",     "19348634",     "22760912",     "24349244",     "17501938",     "18092373",     "18723853",     "18727591",     "20306736",     "24413678",     "24887225",     "25046734",     "16964326",     "16964793",     "16967028",     "16967664",     "16970113",     "16970468",     "20172263",     "20176820",     "25094432",     "25100013",     "21319401",     "21323220",     "22508622",     "23464330",     "23814139",     "24805845",     "14634064",     "15496764",     "15497812",     "15551876",     "15552813",     "15553101",     "15601402",     "15601627",     "15603057",     "15605248",     "15650686",     "15650911",     "15651136",     "16603141",     "16604493",     "16604758",     "16604977",     "16610918",     "16615566",     "16919763",     "16924992",     "17062237",     "17126414",     "17343129",     "17474007",     "17551393",     "18266476",     "18266946",     "19360416",     "19503912",     "22050447",     "22254619",     "22452156",     "22490595",     "17767494",     "17771008",     "15154763",     "15729735",     "15735834",     "19155222",     "19167685",     "19174873",     "19660089",     "19712651",     "19763699",     "19778798",     "19867389",     "19876355",     "19891274",     "21610918",     "22011133",     "22274639",     "22329039",     "22329667",     "24622203",     "24634329",     "24636472",     "24700232",     "24791389",     "25089518",     "25091451",     "25095400",     "25130888",     "25133695",     "17894047",     "22252784",     "20071498",     "24660883",     "24688598",     "15774667",     "24793871",     "24817351",     "24820411",     "24820653",     "24821206",     "24822379",     "24825559",     "22744709",     "15342705",     "15444327",     "15446781",     "16529653",     "16668704",     "16669899",     "16672069",     "16672807",     "16684455",     "16686454",     "16703144",     "16717590",     "17244010",     "17246437",     "17247649",     "17800627",     "17800869",     "19270513",     "19509462",     "19514761",     "19518175",     "19518440",     "19518958",     "19519200",     "19519465",     "19519707",     "19748433",     "19749250",     "19907191",     "20378848",     "21396744",     "21396969",     "21449787",     "21450325",     "21568513",     "21569233",     "21586791",     "21657228",     "21690728",     "21699992",     "21701050",     "21701292",     "21701534",     "21744092",     "21883370",     "21932090",     "21936240",     "21936568",     "21936833",     "21937838",     "22925006",     "22925571",     "24222534",     "24537336",     "24538055",     "24538323",     "24538565",     "24538862",     "24539105",     "24539931",     "24605499",     "24696671",     "24713391",     "24713921",     "24783873",     "24879129",     "24882839",     "24883923",     "24884692",     "24888246",     "24892895",     "24893697",     "24895121",     "24895517",     "24897564",     "24905654",     "24905896",     "24906138",     "24982255",     "25049509",     "25050359",     "25051160",     "25052511",     "25053141",     "25060013",     "25061557",     "25061825",     "25062536",     "25065991",     "25067024",     "25088830",     "25091209",     "25092089",     "25093285",     "25093971",     "25095895",     "25097833",     "25098458",     "25099472",     "25100661",     "25100926",     "15443612",     "15445250",     "15453533",     "15536609",     "15540487",     "15649651",     "15725844",     "16518757",     "16924704",     "16945476",     "16950674",     "17606204",     "17606204",     "17692327",     "17692802",     "17825302",     "17868911",     "20894496",     "20895172",     "20901201",     "21814581",     "21814823",     "21831496",     "23103032",     "24264023",     "24266219",     "24268022",     "25064295",     "18081525",     "22101572",     "22257747",     "23505820",     "15011000",     "17691541",     "19764946",     "20053461",     "20119887",     "20121144",     "20121384",     "20121624",     "20121864",     "20122214",     "20122454",     "20122737",     "20122977",     "20281378",     "20282956",     "20283196",     "20283546",     "20283884",     "20284244",     "20284836",     "20285076",     "20285426",     "20286595",     "20287602",     "21326716",     "21327993",     "21330304",     "21331530",     "21331770",     "21333795",     "21334035",     "21339841",     "21548986",     "21549572",     "21551178",     "21552164",     "21567574",     "21987423",     "21990022",     "23789439",     "24526752",     "15415043",     "15473579",     "15490937",     "15609153",     "16748609",     "16748874",     "16749116",     "17597031",     "19692070",     "20037829",     "20080395",     "21072995",     "21906554",     "21968929",     "22020779",     "22021021",     "22021263",     "22021505",     "22471859",     "22570377",     "22629367",     "22629902",     "24673927",     "24862123",     "15106937",     "17643559",     "19224700",     "17798651",     "17891890",     "18516290",     "18601879",     "15012353",     "15830303",     "17833319",     "17833605",     "19581174",     "19593409",     "19593626",     "19662029",     "19669922",     "19672639",     "19672856",     "19828494",     "19856260",     "20055394",     "17677800",     "20076019",     "22050447",     "23950947",     "22498660",     "24682891",     "25154609",     "15520343",     "17785774",     "21625251",     "13127447",     "15865573",     "20674716",     "22271059",     "16734169",     "17825043",     "23796135",     "21763067",     "16269887",     "16272091",     "16272677",     "16273052",     "19583996",     "19584330",     "19584641",     "19586426",     "19586737",     "19665488",     "19862143",     "21507396",     "21507707",     "21508018",     "21508329",     "21508571",     "21508813",     "21859290",     "21859739",     "20423370",     "22478013",     "22552456",     "23426178",     "24604374",     "15409693",     "15412324",     "15595130",     "15630707",     "15714857",     "15719314",     "17352829",     "17353163",     "17642852",     "17643071",     "17709079",     "17709298",     "17709540",     "18981045",     "20784448",     "20784805",     "20786387",     "20787171",     "22328741",     "24532140",     "24532359",     "25036647",     "21034384",     "17864255",     "18594680",     "22814282",     "19654839",     "19654839",     "13741170",     "14241522",     "14514019",     "15104016",     "15512417",     "15514925",     "15592729",     "16742543",     "16806665",     "17606204",     "17677800",     "17721292",     "18139684",     "18161568",     "18161568",     "19381052",     "19654839",     "19654839",     "19875878",     "19987723",     "20023707",     "20378848",     "20492362",     "20495197",     "20495674",     "20517589",     "21096915",     "21103278",     "21106339",     "21445687",     "21511149",     "21537886",     "21624083",     "21625024",     "21625251",     "21650555",     "21653539",     "21663247",     "21744092",     "21763067",     "21775035",     "21871026",     "21871253",     "22020779",     "22021263",     "22021505",     "22144856",     "22351070",     "22452156",     "22498660",     "22508622",     "22629367",     "22932462",     "23744181",     "24117839",     "24303256",     "24635477",     "24635477",     "24660883",     "24673927",     "24688598",     "24805845",     "25047944",     "25177436",     "17236630",     "17236872",     "17240505",     "17472298",     "21542729",     "21653539",     "21653539",     "21670160",     "21698865",     "24399758",     "24778367",     "21829785",     "15588510",     "17080472",     "17080806",     "16460966",     "16461291",     "16461510",     "16461727",     "16461944",     "16462161",     "17579365",     "17579584",     "19645183",     "19645402",     "19666802",     "19676723",     "19676940",     "19679194",     "19679630",     "19679847",     "19680064",     "19680281",     "21035966",     "21039000",     "21049287",     "22997907",     "22998126",     "25089299",     "25090829",     "25093527",     "25097136",     "13898446",     "24556319",     "18012459",     "22185516",     "13447451",     "13533681",     "13534029",     "13535443",     "14514019",     "14514019",     "16949286",     "16949996",     "16951540",     "16952784",     "16953514",     "16954130",     "17586319",     "17587504",     "17738031",     "17741323",     "17797126",     "17799815",     "17831030",     "17834693",     "17834935",     "17878237",     "17878570",     "17878950",     "17879328",     "17879615",     "17879857",     "17920272",     "17923036",     "17923261",     "18106956",     "18139684",     "18139684",     "18139972",     "18142015",     "18142714",     "18142956",     "19173315",     "19250823",     "19558924",     "19596265",     "19596734",     "19596976",     "19597246",     "19597677",     "19759800",     "19763299",     "19848339",     "19849489",     "19850185",     "19850427",     "21957054",     "21961074",     "21965379",     "21965871",     "22479632",     "22483198",     "22483440",     "22483792",     "22485081",     "23461279",     "24034738",     "24035671",     "24036689",     "24037866",     "24570665",     "24572214",     "24578478",     "24578762",     "24580202",     "24606613",     "24802757",     "24810929",     "24819807",     "24820118",     "24826051",     "24832667",     "24974992",     "24980975",     "24981967",     "25015646",     "25018187",     "15763710",     "20912007",     "15342705",     "17469079",     "17472073",     "21453122",     "21453365",     "24793145",     "24793387",     "24793629",     "24796341",     "24797037",     "17936348",     "21907269",     "15049502",     "15049965",     "15370013",     "15370324",     "15370646",     "15578443",     "15865573",     "17528550",     "19970947",     "21034384",     "22271059",     "24078336",     "19979450",     "22004883",     "22005194",     "22040394",     "22808795",     "15572871",     "18709223",     "18711032",     "18711364",     "18823046",     "22305002",     "21741037",     "21742783",     "21743117",     "22405653",     "25092354",     "15452725",     "15453801",     "15604689",     "17702965",     "24637599",     "17053071",     "17053434",     "17059394",     "20017196",     "24173333",     "25041156",     "17471153",     "17524028",     "20119420",     "21431232",     "22621463",     "22625201",     "24483290",     "15413294",     "17586544",     "17587729",     "17921043",     "18092154",     "19059819",     "19445902",     "20143334",     "21781648",     "22499956",     "24230406",     "24432271",     "24997730",     "25000140",     "15170342",     "16926316",     "17309012",     "17650022",     "17750083",     "21980113",     "21980339",     "22023305",     "22487619",     "22517663",     "22853866",     "23018322",     "23020226",     "23021260",     "23985989",     "24420653",     "24687919",     "15529056",     "15532448",     "15534003",     "15535034",     "15535613",     "15536125",     "15536367",     "15537101",     "17574019",     "17574261",     "17576149",     "17576431",     "17576864",     "17760837",     "20123987",     "21654330",     "24515405",     "24517589",     "19381052",     "21624083",     "22351070",     "17085224",     "17385720",     "17545464",     "21889509",     "21889966",     "22542297",     "19966640",     "22592807",     "23153588",     "24584253",     "24739358",     "24740878",     "19712409",     "15689059",     "17826036",     "17826985",     "17829414",     "17831272",     "19099648",     "19606078",     "19609289",     "19733717",     "19733942",     "20019806",     "20022799",     "20139747",     "21040922",     "21650555",     "21904795",     "21910053",     "24598446",     "24611417",     "24611688",     "24612900",     "24613119",     "24613366",     "24614141",     "24614470",     "13176486",     "13185552",     "15339283",     "15466045",     "15623633",     "15873632",     "16419345",     "16524977",     "17068093",     "17564219",     "18143198",     "18143463",     "18144095",     "22035306",     "25047944",     "15067543",     "15598279",     "15057467",     "15092481",     "15092705",     "15279288",     "17304767",     "17306775",     "19875878",     "21663247",     "21663247",     "23096414",     "24635477",     "24635477",     "24635477",     "19904297",     "21475258",     "24957605",     "25016917",     "25017805",     "25019702",     "22205580",     "24738159",     "15955591",     "16602205",     "24660883",     "24688598",     "22555406",     "24904085",     "13449497",     "22686508",     "24343286",     "24496244",     "24715901",     "24729624",     "22374978",     "24533697",     "15033837",     "17139612",     "17139854",     "17729879",     "18093339",     "18218198",     "19525300",     "19558682",     "19655498",     "19794703",     "19908110",     "20049452",     "20138312",     "20749170",     "20749632",     "21929406",     "22517888",     "24201895",     "24384630",     "24539597",     "21775035",     "17133303",     "16917145",     "17613380",     "22105036",     "22237516",     "22238141",     "19139041",     "15778329",     "15778571",     "16488767",     "16732720",     "16882432",     "17066321",     "17228722",     "17337745",     "17522981",     "17717930",     "17718172",     "17738273",     "17738515",     "19633218",     "19718235",     "20107011",     "21968023",     "22611557",     "24159950",     "24162692",     "15263394",     "17730121",     "21817385",     "21820334",     "21956501",     "22444886",     "22532612",     "22808004",     "23620960",     "24014248",     "22097107",     "22098626",     "24303256",     "19987723",     "22666011",     "15516396",     "15615219",     "17721292",     "17622121",     "19443078",     "19760921",     "21511149",     "21511149",     "21976106",     "22304255",     "22333961",     "22468827",     "22675155",     "22969134",     "23473035",     "24277496",     "24413028",     "24413293",     "24764916",     "15979303",     "16768614",     "17698473",     "17739393",     "18003113",     "24333350",     "24894730",     "15089945",     "16694683",     "19001990",     "24827850",     "20492362",     "20495197",     "20495674",     "21101316",     "24809700",     "19895786",     "20196734",     "24348933",     "24116526",     "24117839",     "22149654",     "15104016",     "25177436",     "17339008",     "17340567",     "21047149",     "18710703",     "13449497",     "13741170",     "15572871",     "20156072",     "22305002",     "15472768",     "15498294",     "21513410",     "21513978",     "21515677",     "24102414",     "25035885",     "25096364",     "16748229",     "16961423",     "17496910",     "17519374",     "19326195",     "19621113",     "20616106",     "21096915",     "21096915",     "21110535",     "22631548",     "24704456",     "19595442",     "20258153",     "20393987",     "20396214",     "21585191",     "21585416",     "21992583",     "22280266",     "22280718",     "22281226",     "22285380",     "22327242",     "22329442",     "22342109",     "22366389",     "22370929",     "22371217",     "22379394",     "22699393",     "22886132",     "23671049",     "24721928",     "24726465",     "25064848",     "25098098",     "25098700",     "25098988",     "25099714",     "25099979",     "25103526",     "25154367",     "15440145",     "16958385",     "18355272",     "18576403",     "18580415",     "18674274",     "21133325",     "21168592",     "21496257",     "21881162",     "21881427",     "21990519",     "22041136",     "22158346",     "22158611",     "22158876",     "22171049",     "22172051",     "22172604",     "22818291",     "23041476",     "23043599",     "23541322",     "24491085",     "24495706",     "24684276",     "24684656",     "24717065",     "24733469",     "15643835",     "16509926",     "22144856",     "14241522",     "16141983",     "16147502",     "16151227",     "16154372",     "16156656",     "16157850",     "16158746",     "16161410",     "16418431",     "16672496",     "16715382",     "16776897",     "17100366",     "17101045",     "17236188",     "17573522",     "17575735",     "17733400",     "17733642",     "17778939",     "17779158",     "17779400",     "17779642",     "17802286",     "17983701",     "17985854",     "17986316",     "18167401",     "19020976",     "19363060",     "19418208",     "19537141",     "19537383",     "19637802",     "20067312",     "20146867",     "20204018",     "20374330",     "20698775",     "21871026",     "21871253",     "21878383",     "22305986",     "22518614",     "24393038",     "24691089",     "15163940",     "20517589",     "22276329",     "24941906",     "24556544",     "25118405");

        
        List<String> pids = processInstances.parallelStream().filter(pi -> {
            Map<String, Object> variables = historyService.
                    createHistoricProcessInstanceQuery().
                    processInstanceId(pi.getId()).
                    includeProcessVariables().
                    singleResult()
                    .getProcessVariables();
            String validazioneSpesaFlag = (String)variables.get("validazioneSpesaFlag");
            FlowsAttachment fileMissione = (FlowsAttachment)variables.get("missioni");
            String statoFinaleDomanda = String.valueOf(variables.get("STATO_FINALE_DOMANDA"));
            
            if (!statoFinaleDomanda.startsWith("FIRMATO"))
                return false;
            
            if (fileMissione == null) {
                log.error("La Process Instance "+ pi.getId() +" non ha l'allegato missioni");
                return false;
            } else {
                if (validazioneSpesaFlag != null && validazioneSpesaFlag.equalsIgnoreCase("si")) {
                    if (fileMissione.getFilename().contains(".signed.signed."))
                        return false;
                } 
                if (fileMissione.getFilename().contains(".signed."))
                    return false;
            }
            return true;
        })
        .map(HistoricProcessInstance::getId)
        .collect(Collectors.toList());
        
        log.info("{}"+pids);

        log.info(""+pids.size());
        
        Map<String, Set<String>> resultUo = new ConcurrentHashMap<>();
        Map<String, Set<String>> resultSpesa = new ConcurrentHashMap<>();
        Map<String, Map<String, Set<String>>> result = new HashMap<>();
        result.put("resultUO", resultUo);
        result.put("resultSpesa", resultSpesa);
        
        pids.forEach(pid -> {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(pid)
                    .includeTaskLocalVariables()
                    .list();
            
            ArrayList<Map<String, Object>> historyForPi = flowsProcessInstanceService.getHistoryForPi(pid);
            Map<String, Object> processVariables = historyService.
                    createHistoricProcessInstanceQuery().
                    processInstanceId(pid).
                    includeProcessVariables().
                    singleResult()
                    .getProcessVariables();
            String validazioneSpesaFlag = (String)processVariables.get("validazioneSpesaFlag");
            boolean flagSpesa = validazioneSpesaFlag != null && validazioneSpesaFlag.equalsIgnoreCase("si");
            boolean firmaSingolaUo = false;
            boolean firmaSingolaSpesa = false;
            
            for (Map<String, Object> historyItem : historyForPi) {
                HistoricTaskInstanceResponse h = (HistoricTaskInstanceResponse) historyItem.get("historyTask");
                Optional<RestVariable> sceltaUtente = h.getVariables().stream().filter(v -> v.getName().equals("sceltaUtente")).findFirst();
                if (!sceltaUtente.isPresent()) {
                    log.error("La variabile sceltaUtente non e' presente per un task nel flusso "+pid);
                    continue;
                }
                boolean firmaSingola = sceltaUtente.get().getValue().equals("Firma");
                List<HistoricIdentityLinkResponse> ids = (List<HistoricIdentityLinkResponse>) historyItem.get("historyIdentityLink");
                Optional<HistoricIdentityLinkResponse> esecutore = ids.stream().filter(i -> i.getType().equals("esecutore")).findFirst();
                String firmatario = esecutore.get().getUserId();
                
                if (firmaSingola) {
                    if (h.getName().equals("FIRMA UO")) {
                        Set<String> processList = resultUo.getOrDefault(firmatario, new HashSet<String>());
                        processList.add(pid);
                        resultUo.put(firmatario, processList);
                    }
                    if (h.getName().equals("FIRMA SPESA")) {
                        Set<String> processList = resultSpesa.getOrDefault(firmatario, new HashSet<String>());
                        processList.add(pid);
                        resultSpesa.put(firmatario, processList);
                    }
                }
            }
            
        });
        
        return ResponseEntity.ok(result);
    }
    
    @RequestMapping(value = "firma-errata-missioni-post", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> postErroriFirmaMissioni(
            @RequestParam(name = "values") String valuesString,
            @RequestParam(name = "testing", defaultValue = "true") boolean testing) {
        Map<String, List<String>> values = new Gson().fromJson(valuesString, HashMap.class);
//        Map<String, List<String>> values =  new ObjectMapper().convertValue(o, new TypeReference<HashMap<String, List<String>>>() {});
        values.forEach((k, v) -> {
            startFirmaMultiplaProcess(k, v, testing);
        });
        return ResponseEntity.ok().build();
    }
    
    private void startFirmaMultiplaProcess(String k, List<String> v, boolean testing) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("titolo", "Firma elenco rimborsi e revoche per missioni");
        data.put("descrizione", "Firma uo / spesa  missioni per rimborsi e revoche precedentemente validate");
        data.put("userNameFirmatario", k);
        
        int i = 0;
        
        for (String id : v) {
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceId(id)
                .singleResult();
            
            Map<String, Object> vars = pi.getProcessVariables();
            
            FlowsAttachment missioni = (FlowsAttachment)vars.get("missioni");
            if (missioni != null) {
                aggiungiDocumento(data, "missioni", missioni, "missioni"+ (i++) );
            } else {
                log.warn("L'allegato obbligatorio missioni era assente per il flusso"+id);
            }
            
            FlowsAttachment anticipoMissione = (FlowsAttachment)vars.get("anticipoMissione");
            if (anticipoMissione != null) {
                aggiungiDocumento(data, "anticipoMissione", anticipoMissione, "allegato"+ (i++) );
            }
        }
        
        log.info("Avvio process Firma Elenco Documenti per l'utente {} e dati {}", k, data);
        if (!testing) {
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("firma-elenco-documenti")
                    .latestVersion()
                    .singleResult();
            data.put("processDefinitionId", pd.getId());
            flowsTaskService.startProcessInstance(pd.getId(), data);
        }
        
    }

    
    
    /**
     * La ddMMyyyy deve essere in format dd/MM/yyyy
     * @param ddMMyyyy La startDate deve essere in format dd/MM/yyyy
     * @return
     * @throws ParseException
     */
    @RequestMapping(value = "firmatario-errato/{ddMMyyyy:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<String>>> getFirmatarioErrato(@PathVariable String ddMMyyyy) throws ParseException {
        
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        Date start = sdf.parse(ddMMyyyy);
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("covid19")
                .unfinished()
                .startedAfter(start)
                .orderByProcessInstanceStartTime().asc()
                .list();
        
        Map<String, BossDto> bossCache = new HashMap<String, BossDto>(); // uso una cache per risparmiare sui roundtrip con ACE
        List<String> results = new ArrayList<String>();
        List<String> errors = new ArrayList<String>();
        String info = "Dal "+ ddMMyyyy +" ad oggi ci sono "+ instances.size() +" flussi ancora attivi, seguono eventuali incongruenze di assegnazioni";
        results.add(info);
        log.info(info);
        
        ForkJoinPool customThreadPool = new ForkJoinPool(6);
        customThreadPool.submit(
                () -> instances.parallelStream().forEach(i -> {
                    String gruppoFirmatarioAttuale = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(i.getId())
                            .variableName("gruppoResponsabileProponente")
                            .singleResult()
                            .getValue()
                            .toString();
                    String initiator = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(i.getId())
                            .variableName("initiator")
                            .singleResult()
                            .getValue()
                            .toString();
                    String gruppoFirmatarioDellUtente = null;
                    String dbsfa = null;
                    String dsfa = null;
                    String cdsuosfa = null;
                    String usernameBoss = null;
                    String dbsfu = null;
                    String dsfu = null;
                    String cdsuosfu = null;
                    try {
                        SimpleEntitaOrganizzativaWebDto strutturaFirmatarioAttuale = aceBridgeService.getStrutturaById(Integer.parseInt(gruppoFirmatarioAttuale.split("@")[1]));
                        dsfa = strutturaFirmatarioAttuale.getDenominazione();
                        cdsuosfa = strutturaFirmatarioAttuale.getCdsuo();
                        BossDto boss = bossCache.computeIfAbsent(initiator, k -> aceBridgeService.bossFirmatarioByUsername(initiator));
                        usernameBoss = boss.getUtente().getUsername();
                        gruppoFirmatarioDellUtente = "responsabile-struttura@"+ boss.getEntitaOrganizzativa().getId();
                        SimpleEntitaOrganizzativaWebDto strutturaFirmatarioDellUtente = aceBridgeService.getStrutturaById(Integer.parseInt(gruppoFirmatarioDellUtente.split("@")[1]));
                        dsfu = strutturaFirmatarioDellUtente.getDenominazione();
                        cdsuosfu = strutturaFirmatarioDellUtente.getCdsuo();
                        if(!gruppoFirmatarioAttuale.equals(gruppoFirmatarioDellUtente)) {
                            String e = "Il flusso "+ i.getId() +" avviato dall'utente "+ initiator
                                    + " il giorno "+ i.getStartTime()
                                    + " è andato al gruppo "+ gruppoFirmatarioAttuale
                                    + " ("+ dbsfa +" - "+ dsfa +" - "+ cdsuosfa +")"
                                    + " invece che a "+ usernameBoss +" del gruppo "+ gruppoFirmatarioDellUtente
                                    + " ("+ dbsfu +" - "+ dsfu +" - "+ cdsuosfu +")";
                            log.info(e);
                            results.add(e);
                        }
                    } catch (Exception e) {
                        String err = "firmatario-errato: Errore nel processamento del flusso "+ i.getId() 
                        +" avviato dall'utente "+initiator
                        +" il giorno "+ i.getStartTime()
                        +" che è andato al gruppo "+ gruppoFirmatarioAttuale
                        + " ("+ dbsfa +" - "+ dsfa +" - "+ cdsuosfa +")"
                        +" invece che a "+ usernameBoss +" del gruppo "+ gruppoFirmatarioDellUtente +")"
                        + " ("+ dbsfu +" - "+ dsfu +" - "+ cdsuosfu +")"
                        +" con messaggio: "+ e.getMessage();
                        log.error(err, e);
                        errors.add(err);
                    }
                })).join();
        
        Map<String, List<String>> result = new HashMap<>();
        result.put("results", results);
        result.put("errors", errors);
        return ResponseEntity.ok(result);
    }
    
    @RequestMapping(value = "addHistoricIdentityLink", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addHistoricIdentityLink(
            @RequestParam("procInstId") String procInstId,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(value = "groupId", required = false) String groupId) {

        AddIdentityLinkForHistoricProcessInstanceCmd cmd = new AddIdentityLinkForHistoricProcessInstanceCmd(procInstId, userId, groupId, Utils.PROCESS_VISUALIZER);
        processEngine.getManagementService().executeCommand(cmd);

        return ResponseEntity.ok().build();
    }
    
    // mtrycz 06/01/21 - metodo disabilitato, ci era servito una volta.
    // @RequestMapping(value = "aggiornaName/{aggiorna}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> agiornaName(@PathVariable("aggiorna") Boolean aggiorna) {
        
        List<HistoricProcessInstance> instances = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceNameLike("{\"stato\":\"\"%") // prendo le PI con lo stato vuoto
            .list();
        
        instances.stream().forEach(pi -> {

            log.info("Processo la ProcessInstance "+ pi.getId() +" con name "+ pi.getName());
            
            HistoricVariableInstance statoFinale = historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(pi.getId())
                    .variableName("statoFinale")
                    .singleResult();
            
            if (statoFinale == null || statoFinale.getValue() == null) {
                log.info("Questa pi non ha lo statoFinale: "+ pi.getId());
                return;
            }
            
            String stato = statoFinale.getValue().toString();
            String name = getName(pi.getId(), stato);
            
            log.info("Inserisco nella ProcessInstance "+ pi.getId() +" il name:"+ name);
            
            if (aggiorna) 
                historyService
                    .createNativeHistoricProcessInstanceQuery()
                    .sql("update act_hi_procinst set name_ = #{piname} where proc_inst_id_ = #{piid} ")
                    .parameter("piname", name)
                    .parameter("piid", pi.getId())
                    .singleResult();
            
            log.info("ProcessInstance "+ pi.getId() +" aggiornata con successo");
        });
        
        return ResponseEntity.ok().build();
    }

    private String getName(String processInstanceId, String stato) {

        String initiator = "";
        String titolo = "";
        String descrizione = "";

        initiator = historyService
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(INITIATOR)
            .singleResult().getValue().toString();
        
        titolo = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(TITOLO)
                .singleResult().getValue().toString();
        
        descrizione = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(DESCRIZIONE)
                .singleResult().getValue().toString();

        org.json.JSONObject name = new org.json.JSONObject();
        name.put(DESCRIZIONE, ellipsis(descrizione, LENGTH_DESCRIZIONE));
        name.put(TITOLO, ellipsis(titolo, LENGTH_TITOLO));
        name.put(STATO, ellipsis(stato, LENGTH_STATO) );
        name.put(INITIATOR, initiator);

        return name.toString();
    }
    
    public class AddIdentityLinkForHistoricProcessInstanceCmd implements Command<Void>, Serializable {

        private static final long serialVersionUID = 1L;

        protected String processInstanceId;

        protected String userId;

        protected String groupId;

        protected String type;

        public AddIdentityLinkForHistoricProcessInstanceCmd(String processInstanceId, String userId, String groupId, String type) {
          validateParams(processInstanceId, userId, groupId, type);
          this.processInstanceId = processInstanceId;
          this.userId = userId;
          this.groupId = groupId;
          this.type = type;
        }

        protected void validateParams(String processInstanceId, String userId, String groupId, String type) {

          if (processInstanceId == null) {
            throw new ActivitiIllegalArgumentException("processInstanceId is null");
          }

          if (type == null) {
            throw new ActivitiIllegalArgumentException("type is required when adding a new process instance identity link");
          }

          if (userId == null && groupId == null) {
            throw new ActivitiIllegalArgumentException("userId and groupId cannot both be null");
          }

        }

        public Void execute(CommandContext commandContext) {

          HistoricProcessInstance processInstance = commandContext.getHistoricProcessInstanceEntityManager().findHistoricProcessInstance(processInstanceId);

          if (processInstance == null) {
            throw new ActivitiObjectNotFoundException("Cannot find process instance with id " + processInstanceId, HistoricProcessInstance.class);
          }

          HistoricIdentityLinkEntity il = new HistoricIdentityLinkEntity();
          il.setProcessInstanceId(processInstanceId);
          il.setGroupId(this.groupId);
          il.setUserId(this.userId);
          il.setType(this.type);

          commandContext.getDbSqlSession().insert(il);
          return null;
        }

      }

    private void aggiungiDocumento(Map params, String tipoDocumento, FlowsAttachment att, String nomeDocumentoFlows){
        params.put(nomeDocumentoFlows+"_label", tipoDocumento);
        params.put(nomeDocumentoFlows+"_nodeRef", att.getUrl());
        params.put(nomeDocumentoFlows+"_mimetype", att.getMimetype());
        params.put(nomeDocumentoFlows+"_aggiorna", "true");
        params.put(nomeDocumentoFlows+"_path", att.getPath());
        params.put(nomeDocumentoFlows+"_filename", att.getFilename());
    }

}
