package it.cnr.si.flows.ng.service;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.RestPdfSiglaService;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test,unittests,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class PdfSiglaServiceTest {

    @Inject
    private RestPdfSiglaService pdfSiglaService;
    @Inject
    private TestServices util;
    private static final String STRINGA_DI_TEST = "<h3 class=\"western\"><span lang=\"en-US\">&#160;<\\/span>State of the art<\\/h3><p class=\"western\"><span lang=\"en-US\">Until a few years ago, superconducting-based electronics was considered an exclusive product of research laboratories. The current situation has radically changed thanks to the development of closed-cycle, compact and light (less than one kg) commercial cryocoolers. These advances have made it possible to market electronic products based on copper oxide high-Tc superconductors. For example, &#34;front-end receivers&#34; for cellular telephony are becoming quite common in commercial networks around the world (<\\/span><font color=\"#1155cc\"><span lang=\"en-US\"><u><a href=\"https:\\/\\/www.hypres.com\\/\">https<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">:\\/\\/<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">www<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">.<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">hypres<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">.<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">com<\\/a><a href=\"https:\\/\\/www.hypres.com\\/\">\\/<\\/a><\\/u><\\/span><\\/font><span lang=\"en-US\">)&#8203;. High-Tc superconductors can also be used in sensing applications like squids and kinetic inductance detectors (KIDs). However, to reduce the cryogenic cost, it would be highly desirable to develop materials with even higher superconducting critical temperatures at ambient pressure which may greatly expand the field of applications.<\\/span><\\/p><p class=\"western\"><br><\\/p><p class=\"western\"><span lang=\"en-US\">Parent copper oxide materials are correlated insulators which become metallic by doping. In the last years, the group in Rome has proposed to develop a new family of materials based on silver fluorides instead of copper oxides. Several experimental studies, in which the CNR groups had a leading role, showed that the electronic structure of the parent stoichiometric compound (AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\">) is indeed very similar to the CuO<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> planes of the parent cuprate compounds<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote1sym\"><sup>1<\\/sup><\\/a><sup><span lang=\"en-US\">,<\\/span><\\/sup><a class=\"sdfootnoteanc\" href=\"#sdfootnote2sym\"><sup>2<\\/sup><\\/a><sup><span lang=\"en-US\">,<\\/span><\\/sup><a class=\"sdfootnoteanc\" href=\"#sdfootnote3sym\"><sup>3<\\/sup><\\/a><span lang=\"en-US\">. Furthermore, theoretical computations predict that AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> based superconductors grown in appropriate substrates should surpass the Tc of cuprates.<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote4sym\"><sup>4<\\/sup><\\/a><span lang=\"en-US\"> Thus, there is a grate interest to synthesize these materials, which may have a strong societal impact. Furthermore, the synthesis of a new family of high-Tc superconductors has been always the source of a scientific revolution in the field. These premises had led to an ongoing national project (PRIN) led by the same Italian PI of this proposal to reach this ambitious goal (<\\/span><font color=\"#1155cc\"><span lang=\"en-US\"><u><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">https<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">:\\/\\/<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">www<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">.<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">cnr<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">.<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">it<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">\\/<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">it<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">\\/<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">progetti<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">-<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">di<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">-<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">ricerca<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">\\/<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">progetto<\\/a><a href=\"https:\\/\\/www.cnr.it\\/it\\/progetti-di-ricerca\\/progetto\\/49182\">\\/49182<\\/a><\\/u><\\/span><\\/font><span lang=\"en-US\">).<\\/span><\\/p><p class=\"western\"><br><\\/p><p class=\"western\"><span lang=\"en-US\">The main challenge of this endeavour is to synthesize AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> in the appropriate substrates and introduce doping to render the films metallic. Bednorz and M&#252;ller reached a similar goal several decades ago<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote5sym\"><sup>5<\\/sup><\\/a><span lang=\"en-US\">. This achievement was reproduced after a few months at Bariloche<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote6sym\"><sup>6<\\/sup><\\/a><span lang=\"en-US\">. The project will allow integrating the Argentinian group to the Italian interdisciplinary team dedicated to the development of AgF2 superconductors. A grate asset of the project will be the long tradition of Bariloche on the development and study of high-Tc superconductors. Both team leaders Yanina Fasano (Bariloche) and Jos&#233; Lorenzana (Rome) are familiar with these developments as Fasano mentor was F. de la Cruz, the leader of the team who first reproduced the Bednorz and M&#252;ller results in the Southern Hemisphere. Instituto Balseiro, Bariloche is the Alma Mater of Jose' Lorenzana, who started his career at the theory group working on the same compounds in parallel with the experimental developments of F. de la Cruz group. He keeps strong ties with Bariloche, for example, a former PhD student from Bariloche (Ojeda Collado) holds now a Marie Curie fellowship at his group in Rome. Furthermore, a PhD student from Fasano group is preparing a Marie Curie project with Luca Camilli and Luca Persichetti who are external collaborators (not listed below as their time is 100% allocated in related projects).&#160;<\\/span><span>Thus, there is a real interest to pursue this collaboration, which can make a significant impact on the scientific community of both institutions.<\\/span><\\/p><p class=\"western\"><br><\\/p><p class=\"western\"><span lang=\"en-US\">Early studies<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote7sym\"><sup>7<\\/sup><\\/a><span lang=\"en-US\"> have shown that it is possible to produce AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> by fluorination of pure Ag. However, <\\/span><span lang=\"en-US\"><b>how this occurs at the microscopic level has not yet been addressed and will be one of our intermediate goals. <\\/b><\\/span><span lang=\"en-US\">An analogous route to produce isoelectronic CuF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> has been studied more recently<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote8sym\"><sup>8<\\/sup><\\/a><span lang=\"en-US\"> but limited to the growth mechanism. CuF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> is much more ionic than AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> and <\\/span><span lang=\"en-US\"><i>J <\\/i><\\/span><span lang=\"en-US\">is in the meV range, so an analogy with cuprates is less obvious. Notwithstanding that, a recent study<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote9sym\"><sup>9<\\/sup><\\/a><span lang=\"en-US\"> suggests that with additional fluorination such analogy and even high-<\\/span><span lang=\"en-US\"><i>T<\\/i><\\/span><sub><span lang=\"en-US\">c<\\/span><\\/sub><span lang=\"en-US\"> superconductivity may arise. Besides this intriguing possibility, CuF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> on the surface of a Cu crystal (Ref. 8, Qiu) is a fascinating system in its own right as it allows studying a Mott insulator, with well localized moments, in contact with a wideband metal. Such combination is known to give rise to fundamental many-body effects as Ruderman&#8211;Kittel&#8211;Kasuya&#8211;Yosida (RKKY) interactions, the Kondo effect and ferromagnetism in magnetic semiconductors. While electronic reconstruction has been studied at the metallic interface of insulating oxides,<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote10sym\"><sup>10<\\/sup><\\/a><span lang=\"en-US\"> <\\/span><span lang=\"en-US\"><b>the Mott-insulator | metallic interface is a new arena which will be explored during the project.<\\/b><\\/span><\\/p><p class=\"western\"><span lang=\"en-US\">Interestingly, the enthalpy of formation of M<\\/span><sub><span lang=\"en-US\">1-x<\\/span><\\/sub><span lang=\"en-US\">F<\\/span><sub><span lang=\"en-US\">x&#160;<\\/span><\\/sub><span lang=\"en-US\">is minimal for the d<\\/span><sup><span lang=\"en-US\">9 <\\/span><\\/sup><span lang=\"en-US\">compounds<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote11sym\"><sup>11<\\/sup><\\/a><span lang=\"en-US\"> meaning that they will form if enough fluorine is present on the surface of the metal as has been demonstrated in Ref. 7 (O&#8217;Donnell) for AgF<\\/span><sub><span lang=\"en-US\">2 <\\/span><\\/sub><span lang=\"en-US\">and Ref. 8 (Qiu) for CuF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\">.<\\/span><\\/p><p class=\"western\"><span lang=\"en-US\">Coming back to bulk AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\">, while<\\/span><span lang=\"en-US\"><i> J<\\/i><\\/span><span lang=\"en-US\"> has been measured (Ref. 1) it is expected that the large spin-orbit (SO) interaction and buckling induces a large Dzyaloshinskii&#8211;Moriya (DM) coupling <\\/span><span lang=\"en-US\"><i><b>D <\\/b><\\/i><\\/span><span lang=\"en-US\"><b>whose magnitude is not yet known. <\\/b><\\/span><span lang=\"en-US\">Furthermore,<\\/span><span lang=\"en-US\"><b> Coulomb parameters like the Hubbard U and the charge transfer energy &#916;<\\/b><\\/span><span lang=\"en-US\"> have been studied by high-energy spectroscopies (Ref. 3) but accurate values as in cuprates<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote12sym\"><sup>12<\\/sup><\\/a><span lang=\"en-US\"> are still lacking for AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\">. <\\/span><span lang=\"en-US\"><b>These gaps in parameters will be addressed, combining theory and experiment.<\\/b><\\/span><\\/p><p class=\"western\"><span lang=\"en-US\">A major open problem is the fate of doped carriers as polaronic self trapping may prevent superconductivity. How to mitigate polaronic tendencies in AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\"> has been studied theoretically<\\/span><a class=\"sdfootnoteanc\" href=\"#sdfootnote13sym\"><sup>13<\\/sup><\\/a><span lang=\"en-US\">, but <\\/span><span lang=\"en-US\"><b>the new &#8220;twist&#8221; introduced by a large <\\/b><\\/span><span lang=\"en-US\"><i><b>D<\\/b><\\/i><\\/span><span lang=\"en-US\"><b> is an open problem.<\\/b><\\/span><span lang=\"en-US\">&#160;These issues will be addressed, combining several experimental probes and theory.<\\/span><\\/p><p class=\"western\"><br><\\/p><p class=\"western\">Originality and innovative aspects of the proposal<\\/p><p class=\"western\"><br><\\/p><p class=\"western\"><span lang=\"en-US\">While multilayers of CuO based materials on <\\/span><span lang=\"en-US\"><i>insulating substrates <\\/i><\\/span><span lang=\"en-US\">are standard by now AgF<\\/span><sub><span lang=\"en-US\">2 <\\/span><\\/sub><span lang=\"en-US\">has only been studied in bulk form with several challenges due to the strong chemical reactivity (Ref. 1-3). The techniques developed will allow us to enhance the properties of AgF<\\/span><sub><span lang=\"en-US\">2 <\\/span><\\/sub><span lang=\"en-US\">layers and also to create multilayers and thin films protected by a cap layer which will be easily handled and usable in applications. We will also study an alternative route to sample preparation, which is immediately available and consist of fluorinating <\\/span><span lang=\"en-US\"><i>metallic substrates<\\/i><\\/span><span lang=\"en-US\">. Such kinds of studies have been done mainly for Cu and only focusing on the width of the CuF<\\/span><sub><span lang=\"en-US\">2 <\\/span><\\/sub><span lang=\"en-US\">layers. Our project will explore the fascinating physics expected at the interface of a wide band metal (Cu and Ag) and a strongly correlated system with localized moments (CuF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\">, AgF<\\/span><sub><span lang=\"en-US\">2<\\/span><\\/sub><span lang=\"en-US\">) opening a new area of research on interface physics with the prospect to find <\\/span><span lang=\"en-US\"><b>high-T<\\/b><\\/span><sub><span lang=\"en-US\"><b>c<\\/b><\\/span><\\/sub><span lang=\"en-US\"><b> superconductivity and exotic magnetism<\\/b><\\/span><span lang=\"en-US\">. To reach our goal of new quantum materials, we will follow an iterative approach: theoretical predictions will guide the synthesis of new materials. Then a characterization will follow and compared with the predictions. Feedback from experiments will allow refining the models leading to new predictions, and closing the loop.<\\/span><\\/p><p class=\"western\"><br><\\/p><p class=\"western\"><span lang=\"en-US\">Theory predicts that the critical temperature for AgF<\\/span><font><span lang=\"en-US\">2 <\\/span><\\/font><span lang=\"en-US\">is as high as 200K for flat monolayer materials, which doubles the best monolayer T<\\/span><font><span lang=\"en-US\">c<\\/span><\\/font><span lang=\"en-US\"> in cuprates (Ref. 4<\\/span><span lang=\"en-US\">). The scientific and technological benefit is apparent, since a world-wide effort will certainly start, with the specific advantage for our national communities to be at the forefront of the new field from the beginning. T<\\/span><font><span lang=\"en-US\">c <\\/span><\\/font><span lang=\"en-US\">of multilayer materials is more difficult to estimate because they depend on a variety of material dependent factors (as interlayer couplings) and a simple relationship as in Ref. 4 (<\\/span><font><span lang=\"en-US\">Grzelak<\\/span><\\/font><span lang=\"en-US\">) can not be done. Still, one should consider that the simplest cuprates are ternary compounds while optimization of T<\\/span><font><span lang=\"en-US\">c <\\/span><\\/font><span lang=\"en-US\">led to quinary compounds (HgBa<\\/span><font><span lang=\"en-US\">2<\\/span><\\/font><span lang=\"en-US\">CaCu<\\/span><font><span lang=\"en-US\">2<\\/span><\\/font><span lang=\"en-US\">O<\\/span><font><span lang=\"en-US\">6+x<\\/span><\\/font><span lang=\"en-US\">). It is clear that also here sarting from a binary (AgF<\\/span><font><span lang=\"en-US\">2<\\/span><\\/font><span lang=\"en-US\">) the room for improvement will be large and room temperature superconductivity will not be unthinkable. The full social and economic impact is premature to value. Nevertheless, a new family of superconductors is almost certain to find at least niche applications, as it happened for cuprated, MgB<\\/span><font><span lang=\"en-US\">2<\\/span><\\/font><span lang=\"en-US\"> and iron-based materials.<\\/span><\\/p><p class=\"western\"><span lang=\"en-US\"><br><\\/span><\\/p><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote1anc\">1<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">. Gawraczy&#324;ki, D. Kurzyd&#322;wski, R. A. Ewings, S. Bandaru, W. Gadomski, Z. Mazej, G. Ruani, I. Bergenti, T. Jaro&#324;, A. Ozarowski, S. Hill, P. J. Leszczy&#324;ki, K. Tok&#225;r, M. Derzsi, P. Barone, K. Wohlfeld, J. Lorenzana, and W. Grochala, Proc. Natl. Acad. Sci. U. S. A. <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>116<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 1495 (2019).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote2anc\">2<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">N. Bachar, K. Koteras, J. Gawraczynski, W. Trzci&#324;ki, J. Paszula, R. Piombo, P. Barone, Z. Mazej, G. Ghiringhelli, A. Nag, K.-J. Zhou, J. Lorenzana, D. van der Marel, and W. Grochala, Phys. Rev. Res. <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>4<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 023108 (2022).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote3anc\">3<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"> R. Piombo, D. Jezierski, H. P. Martins, T. Jaro&#324;, M. N. Gastiasoro, P. Barone, K. Tok&#225;r, P. Piekarz, M. Derzsi, Z. Mazej, M. Abbate, W. Grochala, and J. Lorenzana, <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><i>Strength of Correlations in a Silver Based Cuprate Analogue, arXiv:2204.07400 (2022)<\\/i><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">.<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote4anc\">4<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">A. Grzelak, H. Su, X. Yang, D. Kurzyd&#322;wski, J. Lorenzana, and W. Grochala<\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, Phys. Rev. Mater. <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>4<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 084405 (2020).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote5anc\">5<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">J. G. Bednorz, K. A. M&#252;ller, K. A. Miiller, Z. F&#252;r Phys. B <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>64<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 189 (1986).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote6anc\">6<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">. A. Esparza, C. A. D&#8217;Ovidio, J. Guimpel, E. Osquiguil, L. Civale, and F. de la Cruz, Solid State Commun. 63, 137 (1987).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote7anc\">7<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">P. M. O'Donnell 1970 <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><i>J. Electrochem. Soc.<\\/i><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"> <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>117<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"> 1273<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote8anc\">8<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">S.R. Qiu, H.F. Lai, and J.A. Yarmoff, Phys. Rev. Lett. <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>85<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 1492 (2000); S.R. Qiu and J.A. Yarmoff, Phys. Rev. B <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>63<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 115409 (2001<\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Times New Roman, serif\"><font><span lang=\"en-US\">).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote9anc\">9<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">. Rybin, D. Y. Novoselov, D. M. Korotin, V. I. Anisimov, and A. R. Oganov, Novel Copper Fluoride Analogs of Cuprates, Phys. Chem. Chem. Phys. 23, 15989 (2021).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote10anc\">10<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">A. Brinkman, M. Huijben, M. Van Zalk, J. Huijben, U. Zeitler, J.C. Maan, W.G. Van Der Wiel, G. Rijnders, D.H.A. Blank, and H. Hilgenkamp, Nat. Mater. <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>6<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 493 (2007).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote11anc\">11<\\/a><font color=\"#202124\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><a href=\"https:\\/\\/materialsproject.org\\/\">https<\\/a><a href=\"https:\\/\\/materialsproject.org\\/\">:\\/\\/<\\/a><a href=\"https:\\/\\/materialsproject.org\\/\">materialsproject<\\/a><a href=\"https:\\/\\/materialsproject.org\\/\">.<\\/a><a href=\"https:\\/\\/materialsproject.org\\/\">org<\\/a><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><span lang=\"en-US\"><\\/span><\\/font><\\/p><\\/div><div><p class=\"sdfootnote-western\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote12anc\">12<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">. Ghijsen, L. Tjeng, J. van Elp, H. Eskes, J. Westerink, G. Sawatzky, and M. Czyzyk, Phys. Rev. B <\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\"><b>38<\\/b><\\/span><\\/font><\\/font><\\/font><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">, 11322 (1988).<\\/span><\\/font><\\/font><\\/font><\\/p><\\/div><div><p class=\"western\" align=\"left\"><a class=\"sdfootnotesym-western\" href=\"#sdfootnote13anc\">13<\\/a><font color=\"#000000\"><font face=\"Arial, serif\"><font><span lang=\"en-US\">. Bandaru, M. Derzsi, A. Grzelak, J. Lorenzana, and W. Grochala, Phys. Rev. Mater. 5, 064801 (2021).<\\/span><\\/font><\\/font><\\/font><\\/p><p class=\"sdfootnote-western\"><br><\\/p><\\/div>";
    
    
    @Test
    public void testSiglaPrintShouldFail() throws IOException {
        
        JSONObject valoreParam = new JSONObject();
        valoreParam.put("propostaDiRicerca", STRINGA_DI_TEST);
        
        
        
        JSONObject variabliStampa = new JSONObject();

        variabliStampa.put("nomeFile", "NomeFile");
        variabliStampa.put("report", "/scrivaniadigitale/domandaAccordiBilaterali.jrxml");

        JSONArray array = new JSONArray();

        JSONObject arrayParamsKey = new JSONObject();
        JSONObject nomeParams = new JSONObject();
        arrayParamsKey.put("paramType", "java.lang.String");
        arrayParamsKey.put("valoreParam", valoreParam.toString());
        nomeParams.put("nomeParam", "REPORT_DATA_SOURCE");
        arrayParamsKey.put("key", nomeParams);

        array.put(arrayParamsKey);
        variabliStampa.put("params", array);

        // RICHIESTA DEL PDF
        byte[] pdfByteArray = null;
        pdfByteArray = pdfSiglaService.getSiglaPdf(variabliStampa.toString());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(pdfByteArray.length);
        outputStream.write(pdfByteArray, 0, pdfByteArray.length);
        assertTrue(outputStream.size() > 0);

        util.makeFile(outputStream, "StampaNonSanitizzata.pdf");
    }
    
    @Test
    public void testSiglaPrintSanitized() throws IOException {
        JSONObject valoreParam = new JSONObject();
        valoreParam.put("propostaDiRicerca", Utils.sanitizeHtml(STRINGA_DI_TEST.replaceAll("\\\\/", "/")));
        
        JSONObject variabliStampa = new JSONObject();

        variabliStampa.put("nomeFile", "NomeFile");
        variabliStampa.put("report", "/scrivaniadigitale/domandaAccordiBilaterali.jrxml");

        JSONArray array = new JSONArray();

        JSONObject arrayParamsKey = new JSONObject();
        JSONObject nomeParams = new JSONObject();
        arrayParamsKey.put("paramType", "java.lang.String");
        arrayParamsKey.put("valoreParam", valoreParam.toString());
        nomeParams.put("nomeParam", "REPORT_DATA_SOURCE");
        arrayParamsKey.put("key", nomeParams);

        array.put(arrayParamsKey);
        variabliStampa.put("params", array);

        // RICHIESTA DEL PDF
        byte[] pdfByteArray = null;
        pdfByteArray = pdfSiglaService.getSiglaPdf(variabliStampa.toString());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(pdfByteArray.length);
        outputStream.write(pdfByteArray, 0, pdfByteArray.length);
        assertTrue(outputStream.size() > 0);

        util.makeFile(outputStream, "StampaSanitizzata.pdf");
    }
}