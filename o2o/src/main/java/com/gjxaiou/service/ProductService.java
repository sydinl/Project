package com.gjxaiou.service;


import com.gjxaiou.dto.ImageHolder;
import com.gjxaiou.dto.ProductExecution;
import com.gjxaiou.entity.Product;
import com.gjxaiou.exception.ProductOperationException;

import java.util.List;

public interface ProductService {

	/**
	 * 添加商品信息以及图片处理
	 * 
	 * @param product
	 * @param thumbnail 缩略图
	 * @param productImgHolderList 详情图
	 * @return
	 * @throws ProductOperationException
	 */
	ProductExecution addProduct(Product product, ImageHolder thumbnail, List<ImageHolder> productImgHolderList)
			throws ProductOperationException;

	/**
	 * 通过商品id查询唯一的商品信息
	 * 
	 * @param productId
	 * @return
	 */
	Product getProductById(long productId);

	/**
	 * 修改商品信息以及图片处理
	 * 
	 * @param product
	 * @param thumbnail
	 * @param productImgHolderList
	 * @return
	 * @throws ProductOperationException
	 */
	ProductExecution modifyProduct(Product product, ImageHolder thumbnail, List<ImageHolder> productImgHolderList)
			throws ProductOperationException;

	/**
	 * 查询商品列表并分页，可输入的条件有：商品名（模糊）、商品状态、店铺id、商品类别
	 * 
	 * @param productCondition
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	ProductExecution getProductList(Product productCondition, int pageIndex, int pageSize);

}
