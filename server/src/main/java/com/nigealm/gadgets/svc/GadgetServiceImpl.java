package com.nigealm.gadgets.svc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.nigealm.gadgets.dao.GadgetsRepository;
import com.nigealm.gadgets.dto.GadgetDTO;
import com.nigealm.gadgets.vo.GadgetVO;
import com.nigealm.gadgets.vo.GadgetVO.GadgetGroup;
import com.nigealm.utils.Debug;
import com.nigealm.common.utils.Tracer;

@Consumes(
{ "application/json" })
@Produces(
{ "application/json" })
@Path("/gadgets")
@Service
public class GadgetServiceImpl implements GadgetService
{
	private final static Tracer tracer = new Tracer(GadgetServiceImpl.class);

	private static boolean GADGETS_INITIALIZED = false;

//	@Inject
	private GadgetsRepository gadgetsRepository;

	@Override
	@GET
	@Path("/all")
	@PreAuthorize("hasRole('ROLE_USER')")
	public List<GadgetDTO> getAllGadgets()
	{
		tracer.entry("getAllGadgets");
		List<GadgetVO> gadgetVOs = getGadgetsFromDB();
		Collection<GadgetDTO> gadgetDTOs = convertGadgetVOsToDTOs(gadgetVOs);
		tracer.exit("getAllGadgets");
		return new ArrayList<GadgetDTO>(gadgetDTOs);
	}

	private List<GadgetVO> getGadgetsFromDB()
	{
		tracer.entry("getGadgetsFromDB");

		if (!GADGETS_INITIALIZED)
		{
			tracer.trace("gadgets are not initialized, initializing...");
			// initialize
			initializeGadgets();

			// must be initialized by now
			Debug.assertSG(GADGETS_INITIALIZED);
		}

		tracer.exit("getGadgetsFromDB");

		// return all gadgets
		return (List<GadgetVO>) gadgetsRepository.findAll();
	}

	private void initializeGadgets()
	{
		tracer.entry("initializeGadgets");
		if (GADGETS_INITIALIZED)
			return;

		// first remove all gadgets from db
		gadgetsRepository.deleteAll();

		InputStream resource = null;

		try
		{
			tracer.trace("start reading gadgets file . . . (gadgets.xml)");
			resource = GadgetServiceImpl.class.getClassLoader().getResourceAsStream("gadgets.xml");
			GadgetService.class.getClassLoader();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(resource);
			tracer.trace("finished reading gadgets file . . .");

			NodeList gadgetsList = doc.getElementsByTagName("gadget");
			for (int itr = 0; itr < gadgetsList.getLength(); itr++)
			{
				Node node = gadgetsList.item(itr);
				Element elem = (Element) node;
				String gadgetName = elem.getAttribute("name");
				String gadgetGroup = elem.getAttribute("group");

				NodeList kpisList = elem.getElementsByTagName("kpi");
				StringBuffer kpiString = new StringBuffer();
				for (int itr1 = 0; itr1 < kpisList.getLength(); itr1++)
				{
					Node kpiNode = kpisList.item(itr1);
					Element kpiElement = (Element) kpiNode;
					String textContent = kpiElement.getTextContent();
					kpiString.append(textContent);
					if (itr1 < kpisList.getLength() - 1)
						kpiString.append(", ");
				}

				GadgetVO gadgetVO = new GadgetVO(GadgetGroup.getGroup(gadgetGroup), gadgetName, kpiString.toString());
				gadgetsRepository.save(gadgetVO);
			}
		}
		catch (Exception e)
		{
			Debug.trace(e);
		}
		finally
		{
			if (resource != null)
			{
				try
				{
					resource.close();
				}
				catch (IOException e)
				{
					Debug.trace(e);
				}
			}
		}
		GADGETS_INITIALIZED = true;

		tracer.exit("initializeGadgets");
	}

	/**
	 * Converts GadgetVO ==> GadgetDTO
	 */
	public static Collection<GadgetDTO> convertGadgetVOsToDTOs(final List<GadgetVO> gadgetVOs)
	{
		Collection<GadgetDTO> gadgetDTOs = CollectionUtils.collect(gadgetVOs, new Transformer<GadgetVO, GadgetDTO>()
		{
			public GadgetDTO transform(GadgetVO gadgetVO)
			{
				return new GadgetDTO(gadgetVO.getPrimaryKey(), gadgetVO.getGroup(), gadgetVO.getName(), gadgetVO
						.getKpis());
			}
		});
		return gadgetDTOs;
	}

	/**
	 * Converts GadgetSettingsDTO ==> GadgetSettingsVO
	 */
	public static GadgetVO convertGadgetDTOToVO(GadgetDTO gadgetDTO)
	{
		GadgetVO gadgetVO = new Transformer<GadgetDTO, GadgetVO>()
		{
			public GadgetVO transform(GadgetDTO gadgetDTO)
			{
				GadgetVO gadget = new GadgetVO(gadgetDTO.getGroup(), gadgetDTO.getName(), gadgetDTO.getKpis());
				gadget.setPrimaryKey(Long.valueOf(gadgetDTO.getId()));
				gadget.setGroupId(gadgetDTO.getGroup().getId());
				return gadget;
			}
		}.transform(gadgetDTO);
		return gadgetVO;
	}

}
