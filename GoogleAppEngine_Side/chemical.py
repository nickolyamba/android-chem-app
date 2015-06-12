import webapp2
from google.appengine.ext import ndb
import db_models
import json
import base64
import hashlib

class Chemical(webapp2.RequestHandler):
	def respond(self, code, message, fix=None, dev_mess=None, user_mess=None):
		self.response.status = code
		self.response.status_message = message
		out = {}
		out['developerMessage'] = dev_mess
		out['userMessage'] = user_mess # optional if error msg is not straight enough
		out['errorCode'] = code
		out['errorMessage'] = str(code) + ' ' + message
		out['fix_url'] = fix
		self.response.write(json.dumps(out))

	def root_key(self):
		return ndb.Key("Root", "default")

	def root_key(self, USER_ID):
		return ndb.Key("User", USER_ID)
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"

	#http://stackoverflow.com/questions/1499832/
	def get_auth(self):	  
		USER_ID = None
		TOKEN = None
		if not 'Authorization' in self.request.headers:
			self.response.headers['WWW-Authenticate'] = 'Basic realm="MYREALM"'
			self.response.set_status(401)
			#self.response.out.write("Authorization required")
			self.respond(401, "Authorization required")
		else:
			auth = self.request.headers['Authorization']
			(USER_ID, TOKEN) = base64.b64decode(auth.split(' ')[1]).split(':')
		# Check the username and password, and proceed ...
		return USER_ID, TOKEN

	def authentificate(self, USER_ID, TOKEN):
		# check if user exist in the table
		USER_ID_utf = USER_ID.encode('utf-8')
		TOKEN_utf = TOKEN.encode('utf-8')
		user_in_table = ndb.Key(db_models.User, USER_ID_utf).get()
		if not user_in_table:
			return "noUser"

	def post(self):
		'''
			Creates a Chemical entity

			POST Body Varibales
			name - required
			supplier - required key
			cas - string optional
		'''
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return
 		
		name = self.request.get('name', default_value=None)
		supplier = self.request.get('supplier', default_value=None)
		catalog = self.request.get('catalog', default_value=None)
		cas = self.request.get('cas', default_value=None)
		
		# get supplier key-property
		if supplier:
			if supplier.isdigit():
				supplier_key = ndb.Key(db_models.Supplier, int(supplier), parent=self.root_key(USER_ID))
			else:
				self.respond(400, message="Invalid Request. Supplier id must be an integer")
				return
		else:
			self.respond(400, message="Invalid Request. Supplier id must be entered")
			return

		# create Chemical entity that has supplier as its parent
		chemical = db_models.Chemical(parent=self.root_key(USER_ID))

		chemical.supplier = supplier_key

		if name:
			chemical.name = name	
		else:
			self.respond(400, message="Invalid Request. Chemical name is required")
			return

		if catalog:
			chemical.catalog = catalog	
		else:
			self.respond(400, message="Invalid Request. Catalog number is required")
			return

		if cas:
			chemical.cas = cas
		
		
		# Add Chemical to datastore and return JSON object
		key = chemical.put()
		out = chemical.to_dict()
		self.response.write(json.dumps(out))
		
		return

	######## !!! Replace ancestor in query on login! !!!#########
	def get(self):
		'''
			Returns a List of Chemical entities

			GET 
			None args required
		'''
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		# Query for list of all chemicals and return in json
		qry = db_models.Chemical.query(ancestor=self.root_key(USER_ID)).fetch()
		results = [x for x in qry]
		output = [] # here will be list of suppliers data
		out = {} # dictionary
		for x in results:
			result = x.to_dict()
			output.append(result)
		out['chemicals'] = output
		self.response.write(json.dumps(out))
		
		return


class EditDeleteChem(webapp2.RequestHandler):
	def respond(self, code, message, fix=None, dev_mess=None, user_mess=None):
		self.response.status = code
		self.response.status_message = message
		out = {}
		out['developerMessage'] = dev_mess
		out['userMessage'] = user_mess # optional if error msg is not straight enough
		out['errorCode'] = code
		out['errorMessage'] = str(code) + ' ' + message
		out['fix_url'] = fix
		self.response.write(json.dumps(out))
	
	def root_key_solution(self):
		return ndb.Key("Root", "default")

	def root_key(self, USER_ID):
		return ndb.Key("User", USER_ID)
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"

	#http://stackoverflow.com/questions/1499832/
	def get_auth(self):	  
		USER_ID = None
		TOKEN = None
		if not 'Authorization' in self.request.headers:
			self.response.headers['WWW-Authenticate'] = 'Basic realm="MYREALM"'
			self.response.set_status(401)
			#self.response.out.write("Authorization required")
			self.respond(401, "Authorization required")
		else:
			auth = self.request.headers['Authorization']
			(USER_ID, TOKEN) = base64.b64decode(auth.split(' ')[1]).split(':')
		# Check the username and password, and proceed ...
		return USER_ID, TOKEN

	def authentificate(self, USER_ID, TOKEN):
		# check if user exist in the table
		USER_ID_utf = USER_ID.encode('utf-8')
		TOKEN_utf = TOKEN.encode('utf-8')
		user_in_table = ndb.Key(db_models.User, USER_ID_utf).get()
		if not user_in_table:
			return "noUser"

	def chemical_by_key(self, ident, USER_ID):
		# find the Supplier object to Update
		if ident.isdigit():
			chemical = ndb.Key(db_models.Chemical, int(ident), parent=self.root_key(USER_ID)).get()
			if chemical:
				return chemical
			else:
				self.respond(404, "Not Found. No chemical with id = " + str(ident) + " found",
							'http://localhost:17080/chemical')
				return 0
		else:
			self.respond(404, "No ID provided. ID must be chemical's name or key", 'http://localhost:17080/chemical')
			return 0

	def put(self, **kwargs):
		"""
			Update Chemical entity by its name

			PUT Body Varibales:
			name - String (optional)
			cas - String (optional)
			supplier - String (optional)
			catalog - String (optional)

			Example:
			curl -i -X PUT -d "name=LiOH&cas=652-321-68&supplier=6192449487634432&catalog=20165-50G"/
			-H "Accept: application/json" http://localhost:20080/chemical/5418393301680128
		"""
		# initialize response HTTP headres
		self.headers()

		if "application/json" not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		# get new values to assign to supplier object from request data
		name = self.request.get('name', default_value=None)
		cas = self.request.get('cas', default_value=None)
		new_supplier = self.request.get('supplier', default_value=None)
		#old_supplier = self.request.get('oldSupplier', default_value=None)
		catalog = self.request.get('catalog', default_value=None)
		supplier_key = None
		old_supplier_key = None

		# get supplier key
		#if old_supplier and old_supplier.isdigit():
		#	old_supplier_key = ndb.Key(db_models.Supplier, int(old_supplier), parent=self.root_key())
		
		# create empty chemical entity
		chemical = db_models.Chemical()
		
		if 'cid' in kwargs:
			chemical = self.chemical_by_key(kwargs['cid'], USER_ID)

		if not chemical:
			return
		
		# update chemical entity	
		if name:
			chemical.name = name	
		if cas:
			chemical.cas = cas
		if new_supplier and new_supplier.isdigit():
			chemical.supplier = ndb.Key(db_models.Supplier, int(new_supplier), parent=self.root_key(USER_ID))
		if catalog:
			chemical.catalog = catalog
		# add Chemical object to the datastore and return it as JSON
		key = chemical.put()
		out = chemical.to_dict()
		self.response.write(json.dumps(out))


	def deleteSolution(self, chemID):
		sols = db_models.Solution.query(ancestor=self.root_key_solution()).fetch()
		sol_list = [] # here will be list of solutions edited
		for sol in sols:
			changed = False
			for chem in sol.chemicals:
				if chem.id() == chemID:
					sol.chemicals.remove(chem)
					changed = True
			
			#if changed solution entity, update it
			if changed:				
				sol.put() # update solution
				sol_dict = sol.to_dict()
				sol_dict['removedChemical'] = str(chemID)
				sol_list.append(sol_dict)
		return sol_list

	# delete Chemical entity	
	def delete(self, **kwargs):
		"""
			Delete a Chemical entity by id and Delete chemical in Solution entity
			if it's referenced 

			Delete Varibales:
			id - String

			Example:
			curl -i -X DELETE -H "Accept: application/json" /
			http://localhost:20080/chemical/4642138092470272

			{"chemicals": [{"cas": "265-89-00", "name": "Sulfur", "supplier": 5629499534213120, "
			key": 4642138092470272, "catalog":"fgd-df-85"}],
			 
			"solutions": [{"removedChemical": "4642138092470272", "volume": 25, "solutionID": 1, 
			"chemicals": [5838406743490560], "name": "Solution #1", "key": 5768037999312896}]}
		"""
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		#supplier_key = None
		#supplier = self.request.get('supplier', default_value=None)
		#if supplier and supplier.isdigit():
		#	supplier_key = ndb.Key(db_models.Supplier, int(supplier), parent=self.root_key())
		chemical_key = None
		if 'cid' in kwargs:
			if (kwargs['cid']).isdigit():
				# get chemical obj
				chemical_key = ndb.Key(db_models.Chemical, int(kwargs['cid']), parent=self.root_key(USER_ID))
				chemical = chemical_key.get()
				#self.response.write(chemical)
				if not chemical:
					self.respond(404, "Chemical Not Found. No chemical with ID = " + kwargs['cid'] + " found")
					return
			else:
				self.respond(404, "Not Found. Chemical id must present and be integer")
				return
			
		# delete this key in Solution that has this chemical
		if chemical_key:
			sol_list = self.deleteSolution(chemical_key.id())

		chemical.key.delete() # delete chemical
		# produce JSON
		chem_list = []
		total_dict = {}
		chemical_dict = chemical.to_dict()
		chem_list.append(chemical_dict)
		total_dict['chemicals'] = chem_list
		total_dict['solutions'] = sol_list
		self.response.write(json.dumps(total_dict))
		return


class ChemicalSupplier(webapp2.RequestHandler):
	def respond(self, code, message, fix=None, dev_mess=None, user_mess=None):
		self.response.status = code
		self.response.status_message = message
		out = {}
		out['developerMessage'] = dev_mess
		out['userMessage'] = user_mess # optional if error msg is not straight enough
		out['errorCode'] = code
		out['errorMessage'] = str(code) + ' ' + message
		out['fix_url'] = fix
		self.response.write(json.dumps(out))

	def root_key(self):
		return ndb.Key("Root", "default")

	def root_key(self, USER_ID):
		return ndb.Key("User", USER_ID)
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"

	#http://stackoverflow.com/questions/1499832/
	def get_auth(self):	  
		USER_ID = None
		TOKEN = None
		if not 'Authorization' in self.request.headers:
			self.response.headers['WWW-Authenticate'] = 'Basic realm="MYREALM"'
			self.response.set_status(401)
			#self.response.out.write("Authorization required")
			self.respond(401, "Authorization required")
		else:
			auth = self.request.headers['Authorization']
			(USER_ID, TOKEN) = base64.b64decode(auth.split(' ')[1]).split(':')
		# Check the username and password, and proceed ...
		return USER_ID, TOKEN

	def authentificate(self, USER_ID, TOKEN):
		# check if user exist in the table
		USER_ID_utf = USER_ID.encode('utf-8')
		TOKEN_utf = TOKEN.encode('utf-8')
		user_in_table = ndb.Key(db_models.User, USER_ID_utf).get()
		if not user_in_table:
			return "noUser"
	
	def put(self, **kwargs):
		self.headers()
		'''
		Updated a Supplier entity

		PUT
		name - required
		supplier - required key

		'''
		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		if 'cid' in kwargs:
			if (kwargs['cid']).isdigit():
				# get chemical obj
				chemical = ndb.Key(db_models.Chemical, int(kwargs['cid']), parent=self.root_key(USER_ID)).get()
				if not chemical:
					self.respond(404, "Chemical Not Found. No chemical with = " + kwargs['cid'] + " found")
					return
			else:
				self.respond(404, "Not Found. Chemical id must present and be integer")
				return
		
		if 'sid' in kwargs:
			# get supplier key
			if (kwargs['sid']).isdigit():
				supplier = ndb.Key(db_models.Supplier, int(kwargs['sid']), parent=self.root_key(USER_ID))
				if not supplier:
					self.respond(404, "Supplier Not Found. No supplier with id = " + kwargs['cid'] + " found")
					return
			else:
				self.respond(404, "Not Found. Supplier id must present and must be integer")
				return
		
			chemical.supplier = supplier
			chemical.put()
			self.response.write(json.dumps(chemical.to_dict()))