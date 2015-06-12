from google.appengine.ext import ndb
import hashlib
# Replaced Solvent and Solids with Chemical, since
# physical state doesn't matter and added category
# field to categorize a chemical if it's needed

# Added solutionID because it's the only property
# that makes a solution unique

# Added catNum class

# Source: lecture + reading
# overwriting to_dict()
# http://stackoverflow.com/questions/16850136/ndb-to-dict-method-does-not-include-objects-key
# http://www.blog.pythonlibrary.org/2014/01/21/python-201-what-is-super/
class Model(ndb.Model):
	def to_dict(self):
		# calls parent class method to_dict()
		# that shows attributes besides key 
		#in dictionary format
		d = super(Model, self).to_dict()
		d['key'] = self.key.id()
		return d

class User(Model):
	userID = ndb.StringProperty(required=True)
	hashToken = ndb.StringProperty(required=True)

class Supplier(Model):
	name = ndb.StringProperty(required=True)
	website = ndb.StringProperty()

class Chemical(Model):
	name = ndb.StringProperty(required=True)
	supplier = ndb.KeyProperty(required=True)
	catalog = ndb.StringProperty(required=True)
	cas = ndb.StringProperty()

	def to_dict(self):
		d = super(Chemical, self).to_dict()
		d['supplier'] = (d['supplier']).id()
		return d

class Solution(Model):
	solutionID = ndb.IntegerProperty(required=True)
	name = ndb.StringProperty(required=True)
	volume = ndb.IntegerProperty(required=True)
	chemicals = ndb.KeyProperty(repeated=True)
	#date = ndb.DateProperty(auto_now_add=True)

	def to_dict(self):
		d = super(Solution, self).to_dict()
		d['chemicals'] = [ch.id() for ch in d['chemicals']]
		return d


'''
class HelpFunctions():
	def respond(self, code, message, fix=None):
		self.response.status = code
		self.response.status_message = message
		out = {}
		out['developerMessage'] = "Verbose, plain language description of the problem for the app developer."
		out['userMessage'] = "Pass this message on to the app user if needed."
		out['errorCode'] = code
		out['errorMessage'] = str(code) + ' ' + message
		out['fix'] = fix
		self.response.write(json.dumps(out))

	def root_key(self):
		return ndb.Key("Root", "default")
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"
'''

	
	