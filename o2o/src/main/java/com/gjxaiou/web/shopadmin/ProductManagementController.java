package com.gjxaiou.web.shopadmin;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjxaiou.dto.ImageHolder;
import com.gjxaiou.dto.ProductExecution;
import com.gjxaiou.entity.Product;
import com.gjxaiou.entity.ProductCategory;
import com.gjxaiou.entity.Shop;
import com.gjxaiou.enums.ProductStateEnum;
import com.gjxaiou.exception.ProductOperationException;
import com.gjxaiou.service.ProductCategoryService;
import com.gjxaiou.service.ProductService;
import com.gjxaiou.util.CodeUtil;
import com.gjxaiou.util.HttpServletRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/shopAdmin")
public class ProductManagementController {

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductCategoryService productCategoryService;

	/**
	 * 支持上传商品详情图的最大数量
	 */
	private static final int IMAGE_MAX_COUNT = 6;

	@RequestMapping(value = "/addProduct", method = RequestMethod.POST)
	@ResponseBody
	private Map<String, Object> addProduct(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		if (!CodeUtil.checkVerifyCode(request)) {
			result.put("success", false);
			result.put("errMsg", "输入了错误的验证码");
			return result;
		}
		// 接受前端参数的变量的初始化，包装商品、缩略图、详情图列表实体类
		ObjectMapper mapper = new ObjectMapper();
		Product product = null;
		String productStr = HttpServletRequestUtil.getString(request, "productStr");
		ImageHolder thumbnail = null;
		List<ImageHolder> productImgList = new ArrayList<>();
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		try {
			// 若请求中存在文件流，则取出相关的文件（包括缩略图和详情图）
			if (multipartResolver.isMultipart(request)) {
				thumbnail = handleImage(request, productImgList);
			} else {
				result.put("success", false);
				result.put("errMsg", "上传图片不能为空");
				return result;
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("errMsg", e.toString());
			return result;
		}
		try {
			// 尝试获取前端传过来的表单string流，并将其转换成Product实体类
			product = mapper.readValue(productStr, Product.class);
		} catch (Exception e) {
			result.put("success", false);
			result.put("errMsg", e.toString());
			return result;
		}
		// 若Product信息、缩略图以及详情图里诶包为空，则开始进行商品添加操作
		if (product != null && thumbnail != null && productImgList.size() > 0) {
			try {
				// 从session中获取当前店铺的id并赋值给product，减少对前端数据的依赖
				Shop currentShop = (Shop) request.getSession().getAttribute("currentShop");
				product.setShop(currentShop);
				// 执行添加操作
				ProductExecution pe = productService.addProduct(product, thumbnail, productImgList);
				if (pe.getState() == ProductStateEnum.SUCCESS.getState()) {
					result.put("success", true);
				} else {
					result.put("success", false);
					result.put("errMsg", pe.getStateInfo());
				}
			} catch (ProductOperationException e) {
				result.put("success", false);
				result.put("errMsg", e.toString());
				return result;
			}
		} else {
			result.put("success", false);
			result.put("errMsg", "请输入商品信息");
		}
		return result;
	}

	private ImageHolder handleImage(HttpServletRequest request, List<ImageHolder> productImgList) throws IOException {
		MultipartHttpServletRequest multipartRequest;
		ImageHolder thumbnail;
		multipartRequest = (MultipartHttpServletRequest) request;
		// 取出缩略图并构建ImageHolder对象
		CommonsMultipartFile thumbnailFile = (CommonsMultipartFile) multipartRequest.getFile("thumbnail");
		thumbnail = new ImageHolder(thumbnailFile.getInputStream(), thumbnailFile.getOriginalFilename());
		// 取出详情图列表并构建List<ImageHolder>列表对象，最多支持6张图片上传
		for (int i = 0; i < IMAGE_MAX_COUNT; i++) {
			CommonsMultipartFile productImgFile = (CommonsMultipartFile) multipartRequest.getFile("productImg" + i);
			if (productImgFile != null) {
				// 若取出的详情图片文件流不为空，则将其加入详情图列表
				ImageHolder productImg = new ImageHolder(productImgFile.getInputStream(),
						productImgFile.getOriginalFilename());
				productImgList.add(productImg);
			} else {
				// 若取出的详情图片文件流为空，则终止循环
				break;
			}
		}
		return thumbnail;
	}

	@RequestMapping(value = "/getProductById", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> getProductById(@RequestParam Long productId) {
		Map<String, Object> result = new HashMap<>();
		if (productId > 0) {
			Product product = productService.getProductById(productId);
			if (product != null) {
				List<ProductCategory> productCategoryList = productCategoryService
						.getProductCategoryList(product.getShop().getShopId());
				result.put("product", product);
				result.put("productCategoryList", productCategoryList);
				result.put("success", true);
			} else {
				result.put("product", null);
				result.put("productCategoryList", null);
				result.put("success", true);
			}
		} else {
			result.put("success", false);
			result.put("errMsg", "empty productId");
		}
		return result;
	}

	@RequestMapping(value = "/modifyProduct", method = RequestMethod.POST)
	@ResponseBody
	private Map<String, Object> modifyProduct(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		// 区分是商品编辑还是上下架商品
		boolean statusChange = HttpServletRequestUtil.getBoolean(request, "statusChange");
		// 验证码
		if (!statusChange && !CodeUtil.checkVerifyCode(request)) {
			result.put("success", false);
			result.put("errMsg", "输入了错误的验证码");
			return result;
		}
		// 接受前端参数变量初始化，包括商品、缩略图、详情图列表
		ObjectMapper mapper = new ObjectMapper();
		Product product = null;
		ImageHolder thumbnail = null;
		List<ImageHolder> productImgList = new ArrayList<>();
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		// 若请求中存在文件流，则去除相关的文件

		try {
			if (multipartResolver.isMultipart(request)) {
				thumbnail = handleImage(request, productImgList);
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("errMsg", e.toString());
			return result;
		}
		try {
			String productStr = HttpServletRequestUtil.getString(request, "productStr");
			// 尝试获取前端传过来的表单string流，并将其转换成Product实体类
			product = mapper.readValue(productStr, Product.class);
		} catch (Exception e) {
			result.put("success", false);
			result.put("errMsg", e.toString());
			return result;
		}
		// 若Product信息、缩略图以及详情图里诶包为空，则开始进行商品添加操作
		if (product != null) {
			try {
				// 从session中获取当前店铺的id并赋值给product，减少对前端数据的依赖
				Shop currentShop = (Shop) request.getSession().getAttribute("currentShop");
				product.setShop(currentShop);
				// 进行商品更新
				ProductExecution pe = productService.modifyProduct(product, thumbnail, productImgList);
				if (pe.getState() == ProductStateEnum.SUCCESS.getState()) {
					result.put("success", true);
				} else {
					result.put("success", false);
					result.put("errMsg", pe.getStateInfo());
				}
			} catch (ProductOperationException e) {
				result.put("success", false);
				result.put("errMsg", e.toString());
				return result;
			}
		} else {
			result.put("success", false);
			result.put("errMsg", "请输入商品信息");
		}
		return result;
	}

	@RequestMapping(value = "/getProductListByShop", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> getProductListByShop(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<String, Object>();
		// 获取前台传过来的页码
		int pageIndex = HttpServletRequestUtil.getInt(request, "pageIndex");
		// 获取前台传过来的每页要求返回的商品数
		int pageSize = HttpServletRequestUtil.getInt(request, "pageSize");
		// 从session中获取店铺信息
		Shop currentShop = (Shop) request.getSession().getAttribute("currentShop");
		if ((pageIndex > -1) && (pageSize > -1) && (currentShop != null) && (currentShop.getShopId() != null)) {
			// 获取传入的检索条件
			long productCategoryId = HttpServletRequestUtil.getLong(request, "productCategoryId");
			String productName = HttpServletRequestUtil.getString(request, "productName");
			Product productCondition = compactProductCondition4Search(currentShop.getShopId(), productCategoryId,
					productName);
			// 传入查询条件以及分页信息查询，返回响应商品列表及总数
			ProductExecution pe = productService.getProductList(productCondition, pageIndex, pageSize);
			result.put("productList", pe.getProductList());
			result.put("count", pe.getCount());
			result.put("success", true);
		} else {
			result.put("success", false);
			result.put("errMsg", "empty pageSize or pageIndex or shopId");
		}
		return result;
	}

	/**
	 * 组合查询条件
	 * 
	 * @param shopId
	 * @param productCategoryId
	 * @param productName
	 * @return
	 */
	private Product compactProductCondition4Search(long shopId, long productCategoryId, String productName) {
		Product productCondition = new Product();
		Shop shop = new Shop();
		shop.setShopId(shopId);
		productCondition.setShop(shop);
		if (productCategoryId != -1L) {
			ProductCategory productCategory = new ProductCategory();
			productCategory.setProductCategoryId(productCategoryId);
			productCondition.setProductCategory(productCategory);
		}
		if (productName != null && !productName.equalsIgnoreCase("null")) {
			productCondition.setProductName(productName);
		}
		return productCondition;
	}

}
