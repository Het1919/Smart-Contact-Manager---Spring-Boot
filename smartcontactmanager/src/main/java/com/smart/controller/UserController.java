package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import com.razorpay.*;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model,Principal principal)
	{
        String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		model.addAttribute("user", user);
	}

	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	{
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	//open add form Handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add-contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
			                     @RequestParam("profileImage") MultipartFile file,
			                     Principal principal,
			                     HttpSession session)
	{
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			//processing and uploading file..
			if(file.isEmpty()) {
				//if the file is empty then try our message
				contact.setImage("contact.png");
			}else {
				//upload a file to the folder and update a name to contact
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			user.getContacts().add(contact);
			contact.setUser(user);
			
			this.userRepository.save(user);
			//message success
			session.setAttribute("message", new Message("Your contact is added !! Add more..", "success"));
		} catch (Exception e) {
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("Something went wrong !! Try again", "danger"));
		}
		return "normal/add_contact_form"; 
	}
	
	//show contacts Handler
	//per page = 5[n] contacts
	//current page = 0[page] 
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page")Integer page,Model model,Principal principal)
	{
		model.addAttribute("title", "Show User Contacts");
		//contacts ki list ko bhejni hain
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
	
		return "normal/show_contacts";
	}
	
	//showing particular contact details
	@RequestMapping("/contact/{cId}")
	public String showContactDetail(@PathVariable("cId")Integer cId,Model model,Principal principal) 
	{
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title",contact.getName());
		}
	
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId")Integer cId,Principal principal)
	{
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		//check...
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId")Integer cId,Model model)
	{
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	//update contact handler
	@RequestMapping(value="/process-update",method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage")MultipartFile file,Model m,Principal principal)
	{
		try {
			//old contact details
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty()) {
				//file rewrite ->update new file
				
				//delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile,oldContactDetail.getImage());
				file1.delete();
				
				//update new photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
			}else {
				contact.setImage(oldContactDetail.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/contact/"+contact.getcId();
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model)
	{
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
	
	//open settings handler
	@GetMapping("/settings")
	public String openSettings()
	{
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
								 @RequestParam("newPassword") String newPassword,
								 @RequestParam("confirmNewPassword") String confirmNewPassword,
								 Principal principal,HttpSession session)
	{
		String username = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(username);
		
		if(this.bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword()) && newPassword.equals(confirmNewPassword))
		{
			//change the password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
		}
		else {
			//error...
			session.setAttribute("message", new Message("Password Failed to Update","danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
	
	
	//creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data) throws Exception
	{
		//System.out.println("Hey,Order function executed..");
		//System.out.println(data);
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client = new RazorpayClient("rzp_test_uqRfOOp8tvmXiD", "RMCUpm1dAPzCY2xuWVt1ExmB");
		JSONObject ob = new JSONObject();
		ob.put("amount", amt*100);//paise meh pass karna hain
		ob.put("currency", "INR");
		ob.put("receipt","txn_19012003");
		
		//creating new order
		Order order = client.Orders.create(ob);
		//System.out.println(order);
		
		//if you want then you can save this to your database
		
		return order.toString();
	}
	
}
