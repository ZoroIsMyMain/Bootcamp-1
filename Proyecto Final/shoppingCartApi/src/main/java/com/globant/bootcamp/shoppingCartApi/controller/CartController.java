/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.globant.bootcamp.shoppingCartApi.controller;

import com.globant.bootcamp.shoppingCartApi.model.Cart;
import com.globant.bootcamp.shoppingCartApi.model.Product;
import com.globant.bootcamp.shoppingCartApi.model.ProductCart;
import com.globant.bootcamp.shoppingCartApi.model.User;
import com.globant.bootcamp.shoppingCartApi.service.CartService;
import com.globant.bootcamp.shoppingCartApi.service.ProductService;
import com.globant.bootcamp.shoppingCartApi.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Cristian
 */
@RestController
@RequestMapping(
        path = "api/cart",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "carts", description = "Carts of the cart API", produces = "application/json")
public class CartController {

    private final CartService service;
    private final UserService serviceU;
    private final ProductService serviceP;

    public CartController(final CartService service, final UserService serviceU, final ProductService productP) {
        this.service = service;
        this.serviceU = serviceU;
        this.serviceP = productP;
    }

    @GetMapping("getCarts/{idUser}")
    @ApiOperation(value = "Get User carts", notes = "Returns all Carts of a user")
    @ApiResponses({
        @ApiResponse(code = 204, message = "No carts registred")
        ,@ApiResponse(code = 200, message = "Exits one cart at least")

    })
    public ResponseEntity<List<Cart>> getCartsByClient(@PathVariable("idUser") final Long idUser) throws SQLException {
        List<Cart> cart = service.findAllCartsByClient(idUser);
        if (cart.isEmpty()) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<Cart>>(cart, HttpStatus.OK);
    }

    @GetMapping("getCart/{idCart}")
    @ApiOperation(value = "Get Cart", notes = "Returns Cart")
    @ApiResponses({
        @ApiResponse(code = 404, message = "Cart not found")
        ,@ApiResponse(code = 200, message = "Exits one cart at least")

    })
    public ResponseEntity<Cart> getCart(@PathVariable("idCart") final Long idCart) throws SQLException {
        Cart cart = service.getCart(idCart);
        if (cart.getIdCart() == null) {
            throw new CartController.FoundException("CART NOT FOUND");
        }
        return new ResponseEntity<Cart>(cart, HttpStatus.OK);
    }

    @GetMapping("/getProducts/{idCart}")
    @ApiOperation(value = "Get Products of a cart", notes = "Returns all products of a cart")
    @ApiResponses({
        @ApiResponse(code = 204, message = "No product registred")
        ,@ApiResponse(code = 200, message = "Exits one product at least")

    })
    public ResponseEntity<List<ProductCart>> getProductsByCart(@PathVariable("idCart") final Long idCart) throws SQLException {
        List<ProductCart> cart = service.findAllProductByCart(idCart);
        if (cart.isEmpty()) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<ProductCart>>(cart, HttpStatus.OK);
    }

    @ApiOperation(value = "Save Cart", notes = "add cart to a user")
    @ApiResponses({
        @ApiResponse(code = 409, message = "cart already saved")
        ,@ApiResponse(code = 200, message = "Return the cart and ok")
        ,
             @ApiResponse(code = 404, message = "user not found")

    })
    @PostMapping
    public ResponseEntity<Cart> saveCart(@RequestBody final Cart cart) throws SQLException {
        List<Cart> listCart = service.findAllCartsByClient(cart.getIdUser());
        User u = serviceU.findUsernById(cart.getIdUser());
        if (u.getIdUser() == null) {
            throw new CartController.FoundException("USER NOT FOUND");
        }
        for (Cart obj : listCart) {
            if (obj.isBuyed() == false) {
                throw new CartController.CartExistException("CART ALREADY SAVED BUY THE CART O DELETE THE CART SAVED: ");
            }
            if (cart.getIdCart() == obj.getIdCart()) {
                throw new CartController.CartExistException("CART ID ALREADY EXIST: " + cart.getIdCart());
            }

        }
        service.saveCart(cart);
        return new ResponseEntity<Cart>(cart, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Add ProductCart", notes = "add product to a cart")
    @ApiResponses({
        @ApiResponse(code = 409, message = "product already saved")
        ,@ApiResponse(code = 200, message = "Return the product and ok")
        ,
             @ApiResponse(code = 404, message = "user not found")
        ,@ApiResponse(code = 404, message = "Cart not found")
        ,
             @ApiResponse(code = 404, message = "Product dont exist")
    })
    @PostMapping("/{idProduct}")
    public ResponseEntity<ProductCart> addProductCart(@PathVariable(name = "idProduct") final Long id,
            @RequestBody final ProductCart product) throws SQLException {
        List<ProductCart> listProd = service.findAllProductByCart(product.getIdCart());
        User u = serviceU.findUsernById(product.getIdUser());
        if (u.getIdUser() == null) {
            throw new CartController.FoundException("USER NOT FOUND");
        }
        Cart c = service.getCart(product.getIdCart());
        if (c.getIdCart() == null) {
            throw new CartController.FoundException("CART NOT FOUND");
        }
        Product p = serviceP.findProductById(product.getIdProduct());
        if (p.getIdProduct() == null) {
            throw new CartController.FoundException("PRODUCT DONT EXIST");
        }
        for (ProductCart obj : listProd) {
            if (product.getIdProduct() == obj.getIdProduct()) {
                throw new CartController.CartExistException("PRODUCT ALREADY ADDED: " + product.getIdProduct());
            }

        }

        service.addProductCart(product.getIdProduct(), product);
        service.setTotalCost(product);
        return new ResponseEntity<ProductCart>(service.findProductByCart(product.getIdCart(), product.getIdProduct()), HttpStatus.CREATED);

    }

    @PutMapping("changeQuiantityProduct/{idProduct}")
    @ApiOperation(value = "Modify Product", notes = "Modify quiantity and total import or a Product ")
    @ApiResponses({
        @ApiResponse(code = 404, message = "Product not found")
        ,@ApiResponse(code = 200, message = "Return the product modify and ok")

    })
    public ResponseEntity<ProductCart> putProduct(@PathVariable(name = "idProduct") final Long id,
            @RequestBody ProductCart product) throws SQLException {
        Product p =serviceP.findProductById(product.getIdProduct());
        if (p.getIdProduct() == null) {
            throw new CartController.FoundException("PRODUCT  NOT FOUND: "+id);
        }
        service.changeQuiantityProductCart(product.getIdProduct(), product);
        service.setTotalCost(product);

        return new ResponseEntity<ProductCart>(service.findProductByCart(product.getIdCart(), product.getIdProduct()), HttpStatus.OK);

    }
    @PutMapping("buyCart/{idCart}")
    @ApiOperation(value = "Buy Cart", notes = "Buy a cart ")
    @ApiResponses({
        @ApiResponse(code = 404, message = "Cart not found"),
        @ApiResponse(code = 409, message = "Cart already buyed"),
        @ApiResponse(code = 409, message = "Cart empy")
        ,@ApiResponse(code = 200, message = "Return the product modify and ok")

    })
    public ResponseEntity<Cart> buyCart(@PathVariable(name = "idCart") final Long idCart) throws SQLException {
        Cart c =service.getCart(idCart);
        if (c.getIdCart() == null) {
            throw new CartController.FoundException("CART  NOT FOUND: "+idCart);
        }
        if(c.isBuyed()==true){
            throw new CartController.FoundException("CART  ALREADY BUYED:"+idCart);
        }
        float totalCost=service.getTotalCostCart(c.getIdCart());
        if(totalCost==0){
            throw new CartController.CartExistException("CART IS EMPTY PLEASE ADD PRODUCTS");
        }
        User u=serviceU.findUsernById(c.getIdUser());
        if(totalCost>u.getMonto()){
            throw new CartController.CartExistException("USER CANT BUY THE CART,COST IS BIGGER THAN USER MONEY");
        }
        else{
          float newMontoUser =u.getMonto()-totalCost;
        Cart buyed = service.buyCart(c);
          serviceU.updateUserMoney(newMontoUser, u);

        return new ResponseEntity<Cart>(buyed, HttpStatus.OK);
        } 
    }
    
    @DeleteMapping("/{idProduct}")
    @ApiOperation(value = "Delete ProductCart", notes = "Delete product of a cart")
    @ApiResponses({
            @ApiResponse(code = 404, message = "Product not found"),@ApiResponse(code = 409, message = "Cart is empy"),
        @ApiResponse(code = 404, message = "Cart already buyed"),
        @ApiResponse(code = 200, message = "Return  product Deleted and ok")
           
    })
    public ResponseEntity<Product> deleteProduct(@PathVariable(name = "idProduct")final Long idProduct,@RequestBody final Long idCart) throws SQLException {
        Product product = serviceP.findProductById(idProduct);
        if (product.getIdProduct()==null) {
            throw new ProductController.ProductFoundException("PRODUCT NOT FOUND: " + idProduct);
        }
          float totalCost=service.getTotalCostCart(idCart);
        if(totalCost==0){
            throw new CartController.CartExistException("CART IS EMPTY PLEASE ADD PRODUCTS");
        }
        Cart c = service.getCart(idCart);
        if (c.isBuyed()==true){
                throw new CartController.FoundException("CART  ALREADY BUYED,CANT DELETE PRODUCT OF A CART BUYED:");  
        }
        service.deleteProductCat(idCart,idProduct);
        return new ResponseEntity<Product>(product, HttpStatus.OK);

    }
    
    
    @DeleteMapping("deleteCart/{idCart}")
    @ApiOperation(value = "Delete Cart", notes = "Delete a cart")
    @ApiResponses({
            @ApiResponse(code = 404, message = "Product not found"),@ApiResponse(code = 409, message = "Cart is empy"),
        @ApiResponse(code = 404, message = "Cart already buyed"),
        @ApiResponse(code = 200, message = "Return  product Deleted and ok")
           
    })
    public ResponseEntity<Cart> deleteCart(@PathVariable(name = "idCart")final long idCart) throws SQLException {
        Cart c = service.getCart(idCart);
        if (c.getIdCart()==null) {
            throw new ProductController.ProductFoundException("CART NOT FOUND: " + idCart);
        }
        
        service.deleteCart(c);
        return new ResponseEntity<Cart>(c, HttpStatus.OK);

    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class CartExistException extends RuntimeException {

        public CartExistException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class FoundException extends RuntimeException {

        public FoundException(String message) {
            super(message);
        }
    }

}
